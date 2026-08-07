package com.soundlog.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.soundlog.app.service.ShazamAccessibilityService

object AppChecklistHelper {

    const val SHAZAM_PACKAGE_NAME = "com.shazam.android"

    data class ChecklistItem(
        val id: String,
        val title: String,
        val description: String,
        val isPassed: Boolean,
        val actionText: String,
        val onAction: (Context) -> Unit
    )

    fun getChecklist(context: Context): List<ChecklistItem> {
        val list = mutableListOf<ChecklistItem>()

        // 1. Shazam 앱 설치 체크
        val isShazamInstalled = isAppInstalled(context, SHAZAM_PACKAGE_NAME)
        list.add(
            ChecklistItem(
                id = "shazam_install",
                title = "Shazam 앱 설치 여부",
                description = if (isShazamInstalled) "Shazam 앱이 설치되어 있습니다." else "음악 식별을 위해 Shazam 공식 앱 설치가 필요합니다.",
                isPassed = isShazamInstalled,
                actionText = "플레이스토어 이동",
                onAction = { ctx -> openPlayStore(ctx, SHAZAM_PACKAGE_NAME) }
            )
        )

        // 2. 접근성 권한 체크
        val isAccessibilityEnabled = ShazamAccessibilityService.isServiceRunning()
        list.add(
            ChecklistItem(
                id = "accessibility_perm",
                title = "접근성(Accessibility) 서비스",
                description = if (isAccessibilityEnabled) "SoundLog 접근성 서비스가 정상 활성화되어 있습니다." else "Shazam UI 자동 조작을 위해 SoundLog 접근성 서비스 권한이 필요합니다.",
                isPassed = isAccessibilityEnabled,
                actionText = "접근성 설정 이동",
                onAction = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            )
        )

        // 3. 배터리 최적화 제외 체크
        val isBatteryOptIgnored = isBatteryOptimizationIgnored(context)
        list.add(
            ChecklistItem(
                id = "battery_opt",
                title = "배터리 최적화 제외 설정",
                description = if (isBatteryOptIgnored) "배터리 최적화 대상에서 제외되어 24시간 가동이 보장됩니다." else "24시간 상시 가동을 위해 배터리 최적화 대상 제외 설정이 필요합니다.",
                isPassed = isBatteryOptIgnored,
                actionText = "배터리 설정 이동",
                onAction = { ctx -> openBatteryOptimizationSettings(ctx) }
            )
        )

        // 4. 알림(Notification) 권한 체크 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isNotificationGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            list.add(
                ChecklistItem(
                    id = "notification_perm",
                    title = "알림(Notification) 권한",
                    description = if (isNotificationGranted) "상단 고정 알림창 표시 권한이 허용되어 있습니다." else "Foreground Service 가동 알림 표시 권한이 필요합니다.",
                    isPassed = isNotificationGranted,
                    actionText = "알림 설정 이동",
                    onAction = { ctx -> openAppNotificationSettings(ctx) }
                )
            )
        }

        // 5. 다른 앱 위에 그리기 (Overlay) 권한 체크
        val canDrawOverlays = Settings.canDrawOverlays(context)
        list.add(
            ChecklistItem(
                id = "overlay_perm",
                title = "다른 앱 위에 그리기 권한",
                description = if (canDrawOverlays) "다른 앱 위에 그리기 권한이 활성화되어 있습니다." else "Shazam 팝업/화면 전환 시 원활한 제어를 위해 권한이 권장됩니다.",
                isPassed = canDrawOverlays,
                actionText = "그리기 설정 이동",
                onAction = { ctx ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    ctx.startActivity(intent)
                }
            )
        )

        return list
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun openPlayStore(context: Context, packageName: String) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    private fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    private fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }

    private fun openAppNotificationSettings(context: Context) {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
