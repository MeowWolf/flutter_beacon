package com.flutterbeacon

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import org.altbeacon.beacon.service.scanner.CycledLeScanner

class BeaconEventChannel : StreamHandler, BroadcastReceiver() {

    private var eventSink: EventSink? = null

    override fun onListen(arguments: Any?, events: EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    @SuppressLint("VisibleForTests")
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.run {
            eventSink?.success(
                mapOf<Any, Any>(
                    KEY to getBooleanExtra(
                        CycledLeScanner.EXTRA_IS_SUCCESS,
                        false
                    )
                )
            )
        }
    }

    companion object {
        private const val tag = "BeaconEvent"
        private const val KEY = "ranging_status"
        private const val BEACON_SCAN_ACTIVITY = "flutter_beacon/beacon_activity_channel"
    }

    val action = CycledLeScanner.ACTION_SCAN_RUN

    val name = BEACON_SCAN_ACTIVITY

}