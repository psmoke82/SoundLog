package com.soundlog.app.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

object ShazamNodeFinder {

    private const val TAG = "ShazamNodeFinder"

    data class RecognitionResult(
        val success: Boolean,
        val artist: String? = null,
        val title: String? = null,
        val isNoMatch: Boolean = false,
        val errorMessage: String? = null
    )

    private val SHAZAM_BUTTON_IDS = listOf(
        "com.shazam.android:id/shazam_button",
        "com.shazam.android:id/touch_to_shazam",
        "com.shazam.android:id/auto_shazam_button",
        "com.shazam.android:id/v_shazam_button",
        "com.shazam.android:id/tag_button",
        "com.shazam.android:id/btn_shazam",
        "com.shazam.android:id/shazam_logo",
        "com.shazam.android:id/shazam_fab",
        "com.shazam.android:id/background_shazam_button"
    )

    private val TITLE_IDS = listOf(
        "com.shazam.android:id/title",
        "com.shazam.android:id/track_title",
        "com.shazam.android:id/song_title",
        "com.shazam.android:id/tv_title",
        "com.shazam.android:id/title_text_view",
        "com.shazam.android:id/heading_title"
    )

    private val ARTIST_IDS = listOf(
        "com.shazam.android:id/artist",
        "com.shazam.android:id/artist_title",
        "com.shazam.android:id/subtitle",
        "com.shazam.android:id/tv_subtitle",
        "com.shazam.android:id/artist_text_view",
        "com.shazam.android:id/heading_subtitle"
    )

    /**
     * 5단계 Fallback 기법을 사용하여 Shazam 버튼을 찾아서 클릭합니다.
     */
    fun findAndClickShazamButton(rootNode: AccessibilityNodeInfo?, service: AccessibilityService): Boolean {
        if (rootNode != null) {
            // 1단계: Resource View ID 목록으로 검색
            for (id in SHAZAM_BUTTON_IDS) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        if (performClickOrParentClick(node)) {
                            Log.i(TAG, "Step 1 Success: Clicked Shazam button by view ID [$id]")
                            return true
                        }
                    }
                }
            }

            // 2단계: contentDescription 키워드 검색
            val descKeywords = listOf(
                "shazam", "tap to shazam", "샤잠", "터치하여 shazam",
                "listen", "listening", "shazaming", "touch to shazam", "shazam button"
            )
            val byDesc = findNodesByContentDescription(rootNode, descKeywords)
            for (node in byDesc) {
                if (performClickOrParentClick(node)) {
                    Log.i(TAG, "Step 2 Success: Clicked Shazam button by contentDescription [${node.contentDescription}]")
                    return true
                }
            }

            // 3단계: Text 키워드 검색
            val textKeywords = listOf("shazam", "tap to shazam", "샤잠", "터치하여", "터치")
            val byText = findNodesByTextKeywords(rootNode, textKeywords)
            for (node in byText) {
                if (performClickOrParentClick(node)) {
                    Log.i(TAG, "Step 3 Success: Clicked Shazam button by text [${node.text}]")
                    return true
                }
            }

            // 4단계: Clickable 속성을 가진 대형 중앙 노드 탐색
            val clickableNode = findCentralClickableNode(rootNode)
            if (clickableNode != null && performClickOrParentClick(clickableNode)) {
                Log.i(TAG, "Step 4 Success: Clicked central clickable node")
                return true
            }
        }

        // 5단계: 화면 중앙 좌표 터치 (Fallback Gesture)
        Log.w(TAG, "Step 5 Fallback: Dispatching gesture tap to screen center")
        return dispatchCenterTap(service)
    }

    /**
     * Shazam 화면에서 곡명과 아티스트 텍스트를 파싱합니다.
     */
    fun extractRecognitionResult(rootNode: AccessibilityNodeInfo?): RecognitionResult {
        if (rootNode == null) return RecognitionResult(false, errorMessage = "Root node is null")

        // 1. 인식 실패 화면 확인
        if (isNoMatchScreen(rootNode)) {
            return RecognitionResult(false, isNoMatch = true, errorMessage = "음악을 인식하지 못함 (No Match)")
        }

        // 2. ID 목록 기반 추출
        var title: String? = null
        var artist: String? = null

        for (id in TITLE_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            val t = nodes?.firstOrNull()?.text?.toString()?.trim()
            if (!t.isNullOrBlank() && !isIgnoredText(t)) {
                title = t
                break
            }
        }

        for (id in ARTIST_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            val a = nodes?.firstOrNull()?.text?.toString()?.trim()
            if (!a.isNullOrBlank() && !isIgnoredText(a)) {
                artist = a
                break
            }
        }

        if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            Log.i(TAG, "Extracted by View ID -> Title: $title, Artist: $artist")
            return RecognitionResult(true, artist = artist, title = title)
        }

        // 3. Fallback: 노드 트리를 탐색하여 유효한 텍스트 파싱
        val allTextNodes = mutableListOf<String>()
        collectCleanTextStrings(rootNode, allTextNodes)

        // 텍스트 필터링
        val validTexts = allTextNodes.filter { text ->
            !isIgnoredText(text) && text.length >= 2
        }.distinct()

        if (validTexts.size >= 2) {
            val candidateTitle = validTexts[0]
            val candidateArtist = validTexts[1]
            Log.i(TAG, "Extracted by Text Nodes Fallback -> Title: $candidateTitle, Artist: $candidateArtist")
            return RecognitionResult(true, artist = candidateArtist, title = candidateTitle)
        } else if (validTexts.size == 1 && !title.isNullOrBlank()) {
            val candidateArtist = validTexts[0]
            if (candidateArtist != title) {
                return RecognitionResult(true, artist = candidateArtist, title = title)
            }
        }

        return RecognitionResult(false, errorMessage = "곡 정보 노드를 탐색하지 못함 (Valid texts count: ${validTexts.size})")
    }

    private fun isNoMatchScreen(rootNode: AccessibilityNodeInfo): Boolean {
        val keywords = listOf(
            "no result", "we couldn't catch that", "try again", "didn't catch that",
            "결과 없음", "음악을 찾지 못했습니다", "다시 시도", "다시 듣기"
        )
        return findNodesByTextKeywords(rootNode, keywords).isNotEmpty()
    }

    private fun isIgnoredText(text: String): Boolean {
        val lower = text.lowercase()
        val ignoredKeywords = listOf(
            "shazam", "search", "library", "charts", "settings",
            "open in", "play full song", "connect", "spotify", "apple music",
            "youtube", "lyrics", "share", "video", "track", "listen", "tap to",
            "listening", "shazaming", "찾는 중", "듣는 중"
        )
        return ignoredKeywords.any { lower.contains(it) } || text.length < 2
    }

    private fun performClickOrParentClick(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isClickable) {
                val res = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (res) return true
            }
            current = current.parent
            depth++
        }
        return false
    }

    private fun findNodesByContentDescription(
        root: AccessibilityNodeInfo,
        keywords: List<String>
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun search(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val desc = node.contentDescription?.toString()?.lowercase()
            if (desc != null && keywords.any { desc.contains(it) }) {
                result.add(node)
            }
            for (i in 0 until node.childCount) {
                search(node.getChild(i))
            }
        }
        search(root)
        return result
    }

    private fun findNodesByTextKeywords(
        root: AccessibilityNodeInfo,
        keywords: List<String>
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun search(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val txt = node.text?.toString()?.lowercase()
            if (txt != null && keywords.any { txt.contains(it) }) {
                result.add(node)
            }
            for (i in 0 until node.childCount) {
                search(node.getChild(i))
            }
        }
        search(root)
        return result
    }

    private fun findCentralClickableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var maxNode: AccessibilityNodeInfo? = null
        var maxArea = 0

        fun search(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.isClickable && node.isVisibleToUser) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                val area = rect.width() * rect.height()
                if (area > maxArea && area < 1000000) {
                    maxArea = area
                    maxNode = node
                }
            }
            for (i in 0 until node.childCount) {
                search(node.getChild(i))
            }
        }
        search(root)
        return maxNode
    }

    private fun collectCleanTextStrings(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        if (node.isVisibleToUser) {
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                list.add(text)
            }
        }
        for (i in 0 until node.childCount) {
            collectCleanTextStrings(node.getChild(i), list)
        }
    }

    private fun dispatchCenterTap(service: AccessibilityService): Boolean {
        val displayMetrics = service.resources.displayMetrics
        val centerX = (displayMetrics.widthPixels / 2).toFloat()
        val centerY = (displayMetrics.heightPixels / 2).toFloat()

        val path = Path().apply {
            moveTo(centerX, centerY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }
}
