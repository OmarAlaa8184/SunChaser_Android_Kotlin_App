package com.example.sunchaser.alertFeature.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "STOP_ALARM") {
            context.stopService(Intent(context, AlertService::class.java))
        }
    }
}