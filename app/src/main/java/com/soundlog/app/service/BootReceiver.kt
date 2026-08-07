package com.soundlog.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.soundlog.app.SoundLogApp

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "BootReceiver triggered with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val settings = SoundLogApp.instance.settingsRepository
            if (settings.isServiceEnabled) {
                Log.i(TAG, "Starting ForegroundSchedulerService after device boot/update...")
                ForegroundSchedulerService.startService(context)
            } else {
                Log.i(TAG, "Service is disabled in settings. Skipping boot start.")
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
