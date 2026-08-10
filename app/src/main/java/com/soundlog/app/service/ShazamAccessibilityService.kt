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
                    closeShazamAndReturnToSoundLog()
                    callback(result)
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
                closeShazamAndReturnToSoundLog()
                val res = finalResult ?: ShazamNodeFinder.RecognitionResult(false, errorMessage = "동적 타임아웃(${maxTimeoutSeconds}s) 초과 - 인식 실패")
                onResult(res)
            }
        }
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
