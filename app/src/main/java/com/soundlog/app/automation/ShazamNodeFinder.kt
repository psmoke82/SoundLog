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

    /**
     * 5단계 Fallback 기법을 사용하여 Shazam 버튼을 찾아서 클릭합니다.
     */
    fun findAndClickShazamButton(rootNode: AccessibilityNodeInfo?, service: AccessibilityService): Boolean {
        if (rootNode == null) return false

        // 1단계: Resource View ID로 검색
        val byId = rootNode.findAccessibilityNodeInfosByViewId("com.shazam.android:id/shazam_button")
        if (!byId.isNullOrEmpty()) {
            for (node in byId) {
                if (performClickOrParentClick(node)) {
                    Log.i(TAG, "Step 1 Success: Clicked Shazam button by view ID")
                    return true
                }
            }
        }

        // 2단계: contentDescription 검색
        val byDesc = findNodesByContentDescription(rootNode, listOf("shazam", "tap to shazam", "샤잠", "터치하여 shazam"))
        for (node in byDesc) {
            if (performClickOrParentClick(node)) {
                Log.i(TAG, "Step 2 Success: Clicked Shazam button by contentDescription")
                return true
            }
        }

        // 3단계: Text 검색
        val byText = findNodesByTextKeywords(rootNode, listOf("shazam", "tap to shazam", "샤잠", "터치"))
        for (node in byText) {
            if (performClickOrParentClick(node)) {
                Log.i(TAG, "Step 3 Success: Clicked Shazam button by text")
                return true
            }
        }

        // 4단계: Clickable 속성을 가진 대형 중앙 노드 탐색
        val clickableNode = findCentralClickableNode(rootNode)
        if (clickableNode != null && performClickOrParentClick(clickableNode)) {
            Log.i(TAG, "Step 4 Success: Clicked central clickable node")
            return true
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

        // 1. 인식 실패("No Result", "Try again", "찾지 못했습니다") 화면 확인
        if (isNoMatchScreen(rootNode)) {
            return RecognitionResult(false, isNoMatch = true, errorMessage = "음악을 인식하지 못함 (No Match)")
        }

        // 2. ID 기반 추출
        val titleNodes = rootNode.findAccessibilityNodeInfosByViewId("com.shazam.android:id/title")
        val artistNodes = rootNode.findAccessibilityNodeInfosByViewId("com.shazam.android:id/artist")

        val title = titleNodes?.firstOrNull()?.text?.toString()?.trim()
        val artist = artistNodes?.firstOrNull()?.text?.toString()?.trim()

        if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            return RecognitionResult(true, artist = artist, title = title)
        }

        // 3. Fallback: 노드 트리를 탐색하며 상단에 큰 텍스트(Title)와 하단 텍스트(Artist) 파싱
        val allTextNodes = mutableListOf<AccessibilityNodeInfo>()
        collectTextNodes(rootNode, allTextNodes)

        if (allTextNodes.size >= 2) {
            // 가장 유력한 텍스트 2개 선택
            val candidateTitle = allTextNodes[0].text?.toString()?.trim()
            val candidateArtist = allTextNodes[1].text?.toString()?.trim()

            if (!candidateTitle.isNullOrBlank() && !candidateArtist.isNullOrBlank()
                && !isIgnoredText(candidateTitle) && !isIgnoredText(candidateArtist)
            ) {
                return RecognitionResult(true, artist = candidateArtist, title = candidateTitle)
            }
        }

        return RecognitionResult(false, errorMessage = "곡 정보 노드를 찾을 수 없음")
    }

    private fun isNoMatchScreen(rootNode: AccessibilityNodeInfo): Boolean {
        val keywords = listOf(
            "no result", "we couldn't catch that", "try again",
            "결과 없음", "음악을 찾지 못했습니다", "다시 시도"
        )
        return findNodesByTextKeywords(rootNode, keywords).isNotEmpty()
    }

    private fun isIgnoredText(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("shazam") || lower.contains("search") || lower.contains("library") || lower.length < 2
    }

    private fun performClickOrParentClick(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
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
            if (node.isClickable) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                val area = rect.width() * rect.height()
                if (area > maxArea && area < 1000000) { // 적절한 대형 버튼 크기
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

    private fun collectTextNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && node.isVisibleToUser) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            collectTextNodes(node.getChild(i), list)
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
