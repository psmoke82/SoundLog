package com.soundlog.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.soundlog.app.automation.ShazamNodeFinder
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
    private var activeCallback: ((ShazamNodeFinder.RecognitionResult) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "ShazamAccessibilityService Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isMonitoringActive || event == null) return

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
                    minimizeShazamApp()
                    callback(result)
                }
            }
        }
    }

    fun minimizeShazamApp() {
        try {
            val success = performGlobalAction(GLOBAL_ACTION_HOME)
            Log.i(TAG, "Minimizing Shazam app (GLOBAL_ACTION_HOME) -> Success: $success")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to minimize Shazam app", e)
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
        activeCallback = onResult

        activeRecognitionJob = serviceScope.launch {
            Log.d(TAG, "Starting Shazam Recognition Flow...")

            // 1. Shazam 앱 실행
            val launchIntent = packageManager.getLaunchIntentForPackage(AppChecklistHelper.SHAZAM_PACKAGE_NAME)
            if (launchIntent == null) {
                isMonitoringActive = false
                onResult(ShazamNodeFinder.RecognitionResult(false, errorMessage = "Shazam 앱이 단말기에 설치되어 있지 않습니다."))
                return@launch
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
                    Log.i(TAG, "Shazam button clicked successfully!")
                    break
                }
                delay(500)
            }

            if (!clicked) {
                Log.w(TAG, "Shazam 버튼 클릭 실패. 화면 중앙 제스처 터치 강제 실행")
                clicked = ShazamNodeFinder.findAndClickShazamButton(rootInActiveWindow, this@ShazamAccessibilityService)
            }

            Log.i(TAG, "Shazam button clicked status: $clicked. Monitoring results dynamically (Max: ${maxTimeoutSeconds}s)...")

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
                minimizeShazamApp()
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
