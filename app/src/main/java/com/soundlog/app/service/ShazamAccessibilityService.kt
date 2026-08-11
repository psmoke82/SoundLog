package com.soundlog.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.soundlog.app.automation.ShazamNodeFinder
import com.soundlog.app.ui.MainActivity
import com.soundlog.app.util.AppChecklistHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ShazamAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var activeRecognitionJob: Job? = null
    @Volatile private var isMonitoringActive = false
    @Volatile private var isListeningStarted = false
    private var buttonClickedTimestamp = 0L
    private var activeCallback: ((ShazamNodeFinder.RecognitionResult) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "ShazamAccessibilityService Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isMonitoringActive || !isListeningStarted || event == null) return

        // 버튼 클릭 후 최소 1.5초간은 수음 진행 대기 (조기 리턴 방지)
        if (System.currentTimeMillis() - buttonClickedTimestamp < 1500L) {
            return
        }

        val eventPkg = event.packageName?.toString()
        if (eventPkg != AppChecklistHelper.SHAZAM_PACKAGE_NAME) {
            return
        }

        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val rootNode = rootInActiveWindow ?: return
            if (rootNode.packageName?.toString() != AppChecklistHelper.SHAZAM_PACKAGE_NAME) return

            val result = ShazamNodeFinder.extractRecognitionResult(rootNode)
            if (result.success || result.isNoMatch) {
                Log.i(TAG, "Result captured via AccessibilityEvent! Result: $result")
                val callback = activeCallback
                if (callback != null && isMonitoringActive) {
                    isMonitoringActive = false
                    isListeningStarted = false
                    serviceScope.launch {
                        var finalRes = result
                        val settings = com.soundlog.app.SoundLogApp.instance.settingsRepository
                        if (finalRes.success && settings.albumArtOption == com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_SHAZAM) {
                            finalRes = processOptionBAlbumArtCapture(finalRes)
                        }
                        closeShazamAndReturnToSoundLog()
                        callback(finalRes)
                    }
                }
            }
        }
    }

    fun closeShazamAndReturnToSoundLog() {
        // 1. Accessibility BACK 키로 Shazam 화면 닫기
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.i(TAG, "Dispatched GLOBAL_ACTION_BACK to close Shazam screen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform BACK action on Shazam", e)
        }

        // 2. Shazam 백그라운드 프로세스 및 세션 완전 종료
        try {
            val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            am?.killBackgroundProcesses(AppChecklistHelper.SHAZAM_PACKAGE_NAME)
            Runtime.getRuntime().exec("am force-stop ${AppChecklistHelper.SHAZAM_PACKAGE_NAME}")
            Log.i(TAG, "Force-stopped Shazam package cleanly")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force-stop Shazam package", e)
        }

        // 3. SoundLog 메인 앱으로 복귀
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            Log.i(TAG, "Returned to SoundLog app after Shazam cleanup")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to return to SoundLog", e)
            try { performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "ShazamAccessibilityService Interrupted")
        isMonitoringActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoringActive = false
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Shazam 앱을 실행하고 자동 클릭 후 최대 timeoutSeconds 동안 곡 인식 결과를 모니터링합니다.
     */
    fun startRecognitionFlow(
        maxTimeoutSeconds: Int,
        onResult: (ShazamNodeFinder.RecognitionResult) -> Unit
    ) {
        activeRecognitionJob?.cancel()
        isMonitoringActive = true
        isListeningStarted = false
        activeCallback = onResult

        activeRecognitionJob = serviceScope.launch {
            Log.d(TAG, "Starting Shazam Recognition Flow...")

            // 1. Shazam 앱 실행
            val launchIntent = packageManager.getLaunchIntentForPackage(AppChecklistHelper.SHAZAM_PACKAGE_NAME)
            if (launchIntent == null) {
                isMonitoringActive = false
                isListeningStarted = false
                onResult(ShazamNodeFinder.RecognitionResult(false, errorMessage = "Shazam 앱이 단말기에 설치되어 있지 않습니다."))
                return@launch
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)

            // 앱 렌더링 대기 (초기 1초)
            delay(1000)

            // 2. Shazam 버튼 탐색 및 클릭 (최대 4초간 재시도)
            var clicked = false
            val clickStartTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - clickStartTime < 4000L) {
                val rootNode = rootInActiveWindow
                if (ShazamNodeFinder.findAndClickShazamButton(rootNode, this@ShazamAccessibilityService)) {
                    clicked = true
                    buttonClickedTimestamp = System.currentTimeMillis()
                    Log.i(TAG, "Shazam button clicked successfully!")
                    break
                }
                delay(500)
            }

            if (!clicked) {
                Log.w(TAG, "Shazam 버튼 클릭 실패. 화면 중앙 제스처 터치 강제 실행")
                clicked = ShazamNodeFinder.findAndClickShazamButton(rootInActiveWindow, this@ShazamAccessibilityService)
                buttonClickedTimestamp = System.currentTimeMillis()
            }

            // 버튼 클릭 완료 후 수음 모니터링 활성화
            isListeningStarted = true
            Log.i(TAG, "Shazam button clicked status: $clicked. Waiting initial 1.5s for audio capture...")

            // 수음 시작 후 최소 1.5초 대기 (조기 화면 감지 방지)
            delay(1500)

            Log.i(TAG, "Monitoring results dynamically (Max: ${maxTimeoutSeconds}s)...")

            // 3. 동적 타임아웃 수음 (결과 감지 시 즉시 조기 종료)
            val startTime = System.currentTimeMillis()
            val maxTimeoutMs = maxTimeoutSeconds * 1000L

            var finalResult: ShazamNodeFinder.RecognitionResult? = null

            withTimeoutOrNull(maxTimeoutMs) {
                while (isMonitoringActive && System.currentTimeMillis() - startTime < maxTimeoutMs) {
                    val currentRoot = rootInActiveWindow
                    if (currentRoot != null && currentRoot.packageName?.toString() == AppChecklistHelper.SHAZAM_PACKAGE_NAME) {
                        val result = ShazamNodeFinder.extractRecognitionResult(currentRoot)

                        if (result.success || result.isNoMatch) {
                            finalResult = result
                            Log.i(TAG, "Result detected dynamically via polling! Elapsed: ${System.currentTimeMillis() - startTime}ms")
                            break
                        }
                    }

                    delay(500)
                }
            }

            if (isMonitoringActive) {
                isMonitoringActive = false
                isListeningStarted = false

                var res = finalResult ?: ShazamNodeFinder.RecognitionResult(false, errorMessage = "동적 타임아웃(${maxTimeoutSeconds}s) 초과 - 인식 실패")

                // 방안 B: 인식 성공 및 ALBUM_ART_SHAZAM 설정 시 상세 클릭 -> 스와이프 -> 스크린샷 캡처 수행
                val settings = com.soundlog.app.SoundLogApp.instance.settingsRepository
                if (res.success && settings.albumArtOption == com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_SHAZAM) {
                    res = processOptionBAlbumArtCapture(res)
                }

                closeShazamAndReturnToSoundLog()
                onResult(res)
            }
        }
    }

    private suspend fun processOptionBAlbumArtCapture(result: ShazamNodeFinder.RecognitionResult): ShazamNodeFinder.RecognitionResult {
        Log.i(TAG, "Starting Option B: Album Art Capture Flow (Click header -> Swipe -> Take Screenshot)...")
        try {
            // 1. 첨부 3 상세 화면 전환 대기 (1.2초)
            delay(1200)

            // 2. 첨부 3 상단 아티스트 이미지 영역 클릭 (첨부4로 이동)
            ShazamNodeFinder.clickTopHeaderArea(this)
            delay(1000)

            // 3. 첨부 4 -> 첨부 5 이동을 위해 왼쪽으로 스와이프 (우 -> 좌)
            ShazamNodeFinder.swipeLeftToNextCard(this)
            delay(1000)

            // 4. 스크린샷 캡처 및 이미지 파일 저장
            val imageFile = java.io.File(cacheDir, "cover_art_${System.currentTimeMillis()}.jpg")
            val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()

            ShazamNodeFinder.captureScreenToFile(this, imageFile) { success ->
                deferred.complete(success)
            }

            val captured = kotlinx.coroutines.withTimeoutOrNull(3000L) { deferred.await() } ?: false
            val finalImagePath = if (captured && imageFile.exists()) imageFile.absolutePath else null

            Log.i(TAG, "Option B Album Art Capture completed! Image path: $finalImagePath")
            return result.copy(albumArtPath = finalImagePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed during Option B Album Art Capture", e)
            return result
        }
    }

    fun closeYouTubeAndReturnToSoundLog() {
        // 1. 미디어 재생 일시정지 (PiP 영상 지속 재생 방지)
        try {
            val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
            Log.i(TAG, "Dispatched KEYCODE_MEDIA_PAUSE event to stop YouTube video playback")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send MEDIA_PAUSE key event", e)
        }

        // 2. Accessibility BACK 키 3회 연속 수행 (영상 재생창 -> 검색 화면 -> 유튜브 앱 창 완전히 닫기)
        serviceScope.launch(Dispatchers.Main) {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
                delay(200L)
                performGlobalAction(GLOBAL_ACTION_BACK)
                delay(200L)
                performGlobalAction(GLOBAL_ACTION_BACK)
                Log.i(TAG, "Dispatched 3x GLOBAL_ACTION_BACK to close YouTube activity completely")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform BACK action on YouTube", e)
            }

            // 3. YouTube 백그라운드 프로세스 종료
            try {
                val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                am?.killBackgroundProcesses(com.soundlog.app.automation.YouTubeNodeFinder.YOUTUBE_PACKAGE_NAME)
                Runtime.getRuntime().exec("am force-stop ${com.soundlog.app.automation.YouTubeNodeFinder.YOUTUBE_PACKAGE_NAME}")
            } catch (_: Exception) {}

            delay(200L)

            // 4. SoundLog 메인 앱으로 복귀
            try {
                val intent = Intent(this@ShazamAccessibilityService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                Log.i(TAG, "Returned to SoundLog app after YouTube cleanup")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to return to SoundLog", e)
                try { performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Exception) {}
            }
        }
    }

    suspend fun fetchYouTubeLinkFlow(artist: String, title: String): String {
        Log.i(TAG, "Starting YouTube Link Flow for: $artist - $title")
        val encodedQuery = try {
            java.net.URLEncoder.encode("$artist $title official", "UTF-8")
        } catch (e: Exception) {
            "$artist+$title+official"
        }
        val fallbackUrl = "https://www.youtube.com/results?search_query=$encodedQuery"

        val extractedUrl = withTimeoutOrNull(15000L) {
            try {
                // 1. Intent(Intent.ACTION_SEARCH) 실행
                val launched = com.soundlog.app.automation.YouTubeNodeFinder.launchYouTubeSearch(
                    this@ShazamAccessibilityService,
                    artist,
                    title
                )
                if (!launched) return@withTimeoutOrNull null

                // 2. 검색 결과 화면 렌더링 대기 후 첫 번째 동영상 항목 클릭 -> 영상 재생 화면 이동
                delay(1200L)
                var videoClicked = false
                val startVideoTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startVideoTime < 3000L) {
                    val root = rootInActiveWindow
                    if (root != null && com.soundlog.app.automation.YouTubeNodeFinder.findAndClickFirstVideoItem(root, this@ShazamAccessibilityService)) {
                        videoClicked = true
                        break
                    }
                    delay(300L)
                }
                if (!videoClicked) return@withTimeoutOrNull null

                // 3. 동영상 재생 화면 렌더링 대기 후 '공유' 버튼 클릭
                delay(1000L)
                var shareClicked = false
                val startShareTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startShareTime < 2500L) {
                    val root = rootInActiveWindow
                    if (root != null && com.soundlog.app.automation.YouTubeNodeFinder.clickShareButton(root, this@ShazamAccessibilityService)) {
                        shareClicked = true
                        break
                    }
                    delay(300L)
                }
                if (!shareClicked) return@withTimeoutOrNull null

                // 4. 바텀 시트/팝업에서 '링크 복사' 버튼 클릭
                delay(500L)
                var copyClicked = false
                val startCopyTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startCopyTime < 2500L) {
                    val root = rootInActiveWindow
                    if (root != null && com.soundlog.app.automation.YouTubeNodeFinder.clickCopyLinkButton(root, this@ShazamAccessibilityService)) {
                        copyClicked = true
                        break
                    }
                    delay(300L)
                }
                if (!copyClicked) {
                    closeYouTubeAndReturnToSoundLog()
                    return@withTimeoutOrNull null
                }

                // 5. [Focus Shift] 유튜브 앱 완전 종료 및 SoundLog 앱으로 포커스 복귀
                Log.i(TAG, "Copy link clicked! Closing YouTube and shifting focus to SoundLog before reading clipboard...")
                closeYouTubeAndReturnToSoundLog()
                
                // SoundLog 앱이 화면 포커스(Window Focus)를 획득할 때까지 500ms 대기
                delay(500L)

                // 6. SoundLog가 포커스를 얻은 상태에서 클립보드 실제 URL 추출
                val url = com.soundlog.app.automation.YouTubeNodeFinder.getClipboardUrlWithRetry(this@ShazamAccessibilityService, maxWaitMs = 3000L)
                url
            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchYouTubeLinkFlow", e)
                try { closeYouTubeAndReturnToSoundLog() } catch (_: Exception) {}
                null
            }
        }

        val finalUrl = extractedUrl ?: fallbackUrl
        Log.i(TAG, "YouTube Link Flow finished! Result URL: $finalUrl (Extracted: ${extractedUrl != null})")
        return finalUrl
    }

    companion object {

        private const val TAG = "ShazamAccessibility"

        @Volatile
        var instance: ShazamAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean {
            return instance != null
        }
    }
}
