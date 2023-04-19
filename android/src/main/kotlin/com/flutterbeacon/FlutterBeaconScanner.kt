package com.flutterbeacon

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import org.altbeacon.beacon.Region
import java.lang.ref.WeakReference
import java.util.Calendar

internal class FlutterBeaconScanner(
    val plugin: FlutterBeaconPlugin,
    private val context: Context
) {

    var eventSinkRanging: EventSink? = null
    var eventSinkMonitoring: EventSink? = null
    private var regionRanging: ArrayList<Region>? = null
    private var regionMonitoring: ArrayList<Region>? = null


    private fun startService(key: Long) {
        startBeaconService(ACTION_START) {
            putExtra(KEY_SCANNER, key)
        }
    }

    @JvmField
    val rangingStreamHandler: StreamHandler = object : StreamHandler {
        override fun onListen(o: Any, eventSink: EventSink) {
            Log.d("RANGING", "Start ranging = $o")
            startRanging(o, eventSink)
        }

        override fun onCancel(o: Any) {
            Log.d("RANGING", "Stop ranging = $o")
            stopRanging()
        }
    }

    private fun startRanging(o: Any, eventSink: EventSink) {
        if (o is List<*>) {
            if (regionRanging == null) {
                regionRanging = ArrayList()
            } else {
                regionRanging!!.clear()
            }
            for (item in o) {
                if (item is Map<*, *>) {
                    val region = FlutterBeaconUtils.regionFromMap(item)
                    if (region != null) {
                        regionRanging!!.add(region)
                    }
                }
            }
        } else {
            eventSink.error("Beacon", "invalid region for ranging", null)
            return
        }
        eventSinkRanging = eventSink
        startBeaconService(ACTION_START_RANGING, regionRanging)
    }


    fun stopRanging() {
        startBeaconService(ACTION_STOP_RANGING)
        eventSinkRanging = null
    }

    @JvmField
    val monitoringStreamHandler: StreamHandler = object : StreamHandler {
        override fun onListen(o: Any, eventSink: EventSink) {
            startMonitoring(o, eventSink)
        }

        override fun onCancel(o: Any) {
            stopMonitoring()
        }
    }

    private fun startMonitoring(o: Any, eventSink: EventSink) {
        Log.d(TAG, "START MONITORING=$o")
        if (o is List<*>) {
            if (regionMonitoring == null) {
                regionMonitoring = ArrayList()
            } else {
                regionMonitoring!!.clear()
            }
            for (item in o) {
                if (item is Map<*, *>) {
                    val region = FlutterBeaconUtils.regionFromMap(item)
                    regionMonitoring!!.add(region)
                }
            }
        } else {
            eventSink.error("Beacon", "invalid region for monitoring", null)
            return
        }
        eventSinkMonitoring = eventSink
        startBeaconService(ACTION_START_MONITORING, regionRanging)
    }


    fun stopMonitoring() {
        startBeaconService(ACTION_STOP_MONITORING)
        eventSinkMonitoring = null
    }

    private fun startBeaconService(
        action: String,
        region: java.util.ArrayList<Region>? = null,
        extraDataBlock: Intent.() -> Unit = {},
    ) {
        with(Intent(context, RangingBeaconService::class.java)) {
            putExtra(KEY_ACTION, action)
            extraDataBlock()
            region?.run {
                putParcelableArrayListExtra(KEY_REGIONS, this)
            }
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                context.startForegroundService(this)
            } else {
                context.startService(this)
            }
        }
    }

    init {
        val key = Calendar.getInstance().timeInMillis
        scannerMap[key] = WeakReference(this)
        startService(key)
    }

    companion object {
        private val TAG = FlutterBeaconScanner::class.java.simpleName
        val scannerMap = HashMap<Long, WeakReference<FlutterBeaconScanner>>()
    }
}