package com.soundlog.app.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object YouTubeNodeFinder {
    private const val TAG = "YouTubeNodeFinder"
    const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"


    fun launchYouTubeSearch(context: Context, artist: String, title: String): Boolean {
        return try {
            val searchQuery = "$artist $title official"
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage(YOUTUBE_PACKAGE_NAME)
                putExtra("query", searchQuery)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
            Log.i(TAG, "Successfully launched YouTube search intent with query: $searchQuery")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch YouTube search intent", e)
            false
        }
    }

    /**
     * 유튜브 검색 결과 리스트에서 재생목록/앨범 섹션을 완벽히 스킵하고, 첫 번째 단일 동영상 항목을 찾아 클릭하여 영상 재생 화면으로 이동합니다.
     */
    fun findAndClickFirstVideoItem(
        rootNode: AccessibilityNodeInfo?,
        service: AccessibilityService? = null
    ): Boolean {
        if (rootNode == null) return false

        val rect = Rect()
        val durationRegex = Regex("""\d+:\d+|\d+분\s*\d+초|\d+초""")
        val playlistRegex = Regex("""재생목록|앨범|Playlist|Album|동영상\s*\d+개""")

        // 1. 재생목록 및 앨범 노드 필터링 & 단일 동영상 전용 노드 탐색
        val candidateNodes = findAllNodes(rootNode) { node ->
            val viewId = node.viewIdResourceName ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            val combinedText = "$desc $text"

            node.getBoundsInScreen(rect)
            val isBelowTopBar = rect.top >= 200

            // 1) 재생목록 / 앨범 키워드가 텍스트나 설명에 포함되어 있으면 제외
            val isPlaylistOrAlbum = playlistRegex.containsMatchIn(combinedText)
            if (isPlaylistOrAlbum) {
                return@findAllNodes false
            }

            // 2) 단일 동영상 뷰 ID 또는 재생시간 패턴 확인
            val hasSingleVideoId = viewId.contains("video_micro_format") ||
                    viewId.contains("compact_video_item") ||
                    viewId.contains("reel_shelf_creation_entry_renderer") ||
                    viewId.endsWith(":id/dismissible")

            val hasDurationPattern = durationRegex.containsMatchIn(combinedText)

            isBelowTopBar && (hasSingleVideoId || hasDurationPattern)
        }

        if (candidateNodes.isEmpty()) {
            Log.w(TAG, "Could not find any single video item candidate node")
            return false
        }

        // Y축 위치 기준(y >= 200)으로 가장 상단에 위치한 단일 동영상 카드 선택
        val targetNode = candidateNodes.minByOrNull {
            it.getBoundsInScreen(rect)
            rect.top
        }

        if (targetNode != null) {
            val clicked = performClickWithParentFallback(targetNode, service)
            Log.i(TAG, "Found first single video item node (filtered playlists/albums), click result: $clicked")
            return clicked
        }

        return false
    }

    /**
     * 재생 화면에서 '공유' 버튼 탐색 및 클릭 (클릭 불가능할 경우 Gesture Touch 진행)
     */
    fun clickShareButton(
        rootNode: AccessibilityNodeInfo?,
        service: AccessibilityService? = null
    ): Boolean {
        if (rootNode == null) return false

        val shareNode = findNode(rootNode) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""

            (desc.contains("공유") || desc.contains("Share") || text.contains("공유") || text.contains("Share"))
        }

        if (shareNode != null) {
            val clicked = performClickWithParentFallback(shareNode, service)
            Log.i(TAG, "Found share button node, click result: $clicked")
            return clicked
        }

        Log.w(TAG, "Could not find share button node")
        return false
    }

    /**
     * 바텀 시트/팝업에서 '링크 복사' 버튼 탐색 및 클릭
     */
    fun clickCopyLinkButton(
        rootNode: AccessibilityNodeInfo?,
        service: AccessibilityService? = null
    ): Boolean {
        if (rootNode == null) return false

        val copyNode = findNode(rootNode) { node ->
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""

            (desc.contains("링크 복사") || desc.contains("Copy link") || text.contains("링크 복사") || text.contains("Copy link"))
        }

        if (copyNode != null) {
            val clicked = performClickWithParentFallback(copyNode, service)
            Log.i(TAG, "Found copy link button node, click result: $clicked")
            return clicked
        }

        Log.w(TAG, "Could not find copy link button node")
        return false
    }

    suspend fun getClipboardUrlWithRetry(context: Context, maxWaitMs: Long = 3000L): String? = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()
        var attempt = 0
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            attempt++
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = clipboard?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
                    if (!text.isNullOrBlank() && (text.startsWith("http://") || text.startsWith("https://") || text.contains("youtu.be") || text.contains("youtube.com"))) {
                        Log.i(TAG, "Successfully extracted URL from clipboard on attempt $attempt: $text")
                        return@withContext text
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accessing clipboard on attempt $attempt", e)
            }
            delay(200L)
        }
        Log.w(TAG, "Failed to extract valid HTTP/HTTPS URL from clipboard after ${maxWaitMs}ms ($attempt attempts)")
        return@withContext null
    }

    fun getClipboardUrl(context: Context): String? {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
                if (!text.isNullOrBlank() && (text.startsWith("http://") || text.startsWith("https://"))) {
                    Log.i(TAG, "Extracted URL from clipboard: $text")
                    return text
                }
            }
            Log.w(TAG, "No valid HTTP/HTTPS URL found in clipboard")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing clipboard", e)
            null
        }
    }


    private fun findNode(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) {
                return node
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return null
    }

    private fun findAllNodes(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) {
                result.add(node)
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return result
    }

    private fun performClickWithParentFallback(
        node: AccessibilityNodeInfo,
        service: AccessibilityService? = null
    ): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }
            current = current.parent
        }

        // 클릭 가능한 부모 노드가 없거나 performAction 실패 시 Gesture Touch 진행
        if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val centerX = rect.centerX().toFloat()
                val centerY = rect.centerY().toFloat()
                Log.i(TAG, "Performing fallback gesture click at ($centerX, $centerY)")
                return dispatchGestureClick(service, centerX, centerY)
            }
        }

        return false
    }

    fun dispatchGestureClick(service: AccessibilityService, x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }
}
