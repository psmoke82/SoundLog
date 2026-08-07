package com.soundlog.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.soundlog.app.automation.ShazamNodeFinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ShazamAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var activeRecognitionJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "ShazamAccessibilityService Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event logging if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "ShazamAccessibilityService Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
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
        activeRecognitionJob = serviceScope.launch {
            Log.d(TAG, "Starting Shazam Recognition Flow...")

            // 1. Shazam 앱 실행
            val launchIntent = packageManager.getLaunchIntentForPackage(SHAZAM_PACKAGE_NAME)
            if (launchIntent == null) {
                onResult(ShazamNodeFinder.RecognitionResult(false, errorMessage = "Shazam 앱이 단말기에 설치되어 있지 않습니다."))
                return@launch
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)

            // 앱 렌더링 대기
            delay(1500)

            // 2. Shazam 버튼 탐색 및 클릭
            val rootNode = rootInActiveWindow
            val clicked = ShazamNodeFinder.findAndClickShazamButton(rootNode, this@ShazamAccessibilityService)
            if (!clicked) {
                Log.w(TAG, "Shazam 버튼 클릭 실패. 강제 조기 종료 시도")
                onResult(ShazamNodeFinder.RecognitionResult(false, errorMessage = "Shazam 시작 버튼을 탐색/클릭하지 못함"))
                return@launch
            }

            Log.i(TAG, "Shazam button clicked. Polling for results dynamically (Max: ${maxTimeoutSeconds}s)...")

            // 3. 동적 타임아웃 수음 (결과 감지 시 즉시 조기 종료)
            val startTime = System.currentTimeMillis()
            val maxTimeoutMs = maxTimeoutSeconds * 1000L

            var finalResult: ShazamNodeFinder.RecognitionResult? = null

            withTimeoutOrNull(maxTimeoutMs) {
                while (System.currentTimeMillis() - startTime < maxTimeoutMs) {
                    val currentRoot = rootInActiveWindow
                    val result = ShazamNodeFinder.extractRecognitionResult(currentRoot)

                    if (result.success || result.isNoMatch) {
                        finalResult = result
                        Log.i(TAG, "Result detected dynamically! Elapsed: ${System.currentTimeMillis() - startTime}ms")
                        break
                    }

                    delay(500) // 500ms 주기 폴링
                }
            }

            if (finalResult != null) {
                onResult(finalResult!!)
            } else {
                onResult(ShazamNodeFinder.RecognitionResult(false, errorMessage = "동적 타임아웃(${maxTimeoutSeconds}s) 초과 - 인식 실패"))
            }

            // 4. Shazam 앱 닫기 (홈 화면 이동)
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    companion object {
        private const val TAG = "ShazamAccessibility"
        const val SHAZAM_PACKAGE_NAME = "com.shazam.android"

        @Volatile
        var instance: ShazamAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null
    }
}
