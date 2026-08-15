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
    private var confirmationJob: Job? = null
    @Volatile private var isMonitoringActive = false
    @Volatile private var isListeningStarted = false
    @Volatile private var isFinalizing = false
    private var pendingCandidate: ShazamNodeFinder.RecognitionResult? = null
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
                Log.i(TAG, "Candidate result via AccessibilityEvent: $result")
                onCandidateDetected(result)
            }
        }
    }

    /**
     * 이벤트/폴링 어느 경로에서 감지되었든, 결과를 즉시 확정하지 않고 후보로만 저장합니다.
     * CONFIRM_DELAY_MS 뒤 화면을 한 번 더 읽어 같은 결과가 재현될 때만 finalizeResult()로 확정합니다.
     * (Shazam UI 전환 과정의 찰나의 과도기 프레임이 오탐으로 확정되는 것을 방지)
     */
    private fun onCandidateDetected(candidate: ShazamNodeFinder.RecognitionResult) {
        if (!isMonitoringActive || isFinalizing) return

        val pending = pendingCandidate
        val sameAsPending = pending != null && (
            (pending.success && candidate.success && pending.artist == candidate.artist && pending.title == candidate.title) ||
            (pending.isNoMatch && candidate.isNoMatch)
        )
        if (sameAsPending && confirmationJob?.isActive == true) return

        pendingCandidate = candidate
        confirmationJob?.cancel()
        confirmationJob = serviceScope.launch {
            delay(CONFIRM_DELAY_MS)
            if (!isMonitoringActive || isFinalizing) return@launch

            val currentRoot = rootInActiveWindow
            if (currentRoot == null || currentRoot.packageName?.toString() != AppChecklistHelper.SHAZAM_PACKAGE_NAME) {
                pendingCandidate = null
                return@launch
            }

            val recheck = ShazamNodeFinder.extractRecognitionResult(currentRoot)
            val confirmed = (candidate.success && recheck.success && candidate.artist == recheck.artist && candidate.title == recheck.title) ||
                (candidate.isNoMatch && recheck.isNoMatch)

            if (confirmed) {
                Log.i(TAG, "Candidate confirmed after re-check: $recheck")
                finalizeResult(recheck)
            } else {
                Log.i(TAG, "Candidate NOT reproduced on re-check (discarded as transient frame): $candidate -> $recheck")
                pendingCandidate = null
            }
        }
    }

    /**
     * 인식 흐름을 종료하고 콜백을 호출하는 유일한 지점.
     * isFinalizing 플래그로 이벤트/폴링/타임아웃 경로 중 어느 하나만 종결을 수행하도록 보장합니다.
     */
    private suspend fun finalizeResult(result: ShazamNodeFinder.RecognitionResult) {
        if (isFinalizing) return
        isFinalizing = true
        isMonitoringActive = false
        isListeningStarted = false
        confirmationJob?.cancel()
        pendingCandidate = null

        var finalRes = result
        val settings = com.soundlog.app.SoundLogApp.instance.settingsRepository
        if (finalRes.success && settings.albumArtOption == com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_SHAZAM) {
            finalRes = processOptionBAlbumArtCapture(finalRes)
        }

        closeShazamAndReturnToSoundLog()
        val callback = activeCallback
        activeCallback = null
        callback?.invoke(finalRes)
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
        confirmationJob?.cancel()
        pendingCandidate = null
        isFinalizing = false
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

            withTimeoutOrNull(maxTimeoutMs) {
                while (isMonitoringActive && System.currentTimeMillis() - startTime < maxTimeoutMs) {
                    val currentRoot = rootInActiveWindow
                    if (currentRoot != null && currentRoot.packageName?.toString() == AppChecklistHelper.SHAZAM_PACKAGE_NAME) {
                        val result = ShazamNodeFinder.extractRecognitionResult(currentRoot)

                        if (result.success || result.isNoMatch) {
                            Log.i(TAG, "Candidate result via polling! Elapsed: ${System.currentTimeMillis() - startTime}ms")
                            onCandidateDetected(result)
                        }
                    }

                    delay(500)
                }
            }

            // 확정 권한은 finalizeResult() 단일 지점뿐이므로, 타임아웃까지 아무것도 확정되지 않았을 때만
            // 여기서 타임아웃 실패로 종결합니다. (진행 중인 재확인이 있었다면 그쪽이 이미 처리했을 것)
            if (isMonitoringActive && !isFinalizing) {
                finalizeResult(
                    ShazamNodeFinder.RecognitionResult(false, errorMessage = "동적 타임아웃(${maxTimeoutSeconds}s) 초과 - 인식 실패")
                )
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

    suspend fun fetchYouTubeLinkFlow(artist: String, title: String): String {
        Log.i(TAG, "Generating YouTube Search Link for: $artist - $title")
        return com.soundlog.app.automation.YouTubeNodeFinder.buildYouTubeSearchUrl(artist, title)
    }

    companion object {

        private const val TAG = "ShazamAccessibility"

        // 후보 결과가 실제로 안정된 최종 화면인지 재확인하기까지 대기하는 시간
        private const val CONFIRM_DELAY_MS = 300L

        @Volatile
        var instance: ShazamAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean {
            return instance != null
        }
    }
}
