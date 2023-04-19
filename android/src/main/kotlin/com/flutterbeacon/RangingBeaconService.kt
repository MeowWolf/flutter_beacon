package com.flutterbeacon

import android.R.drawable
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationCompat
import com.flutterbeacon.FlutterBeaconScanner.Companion.scannerMap
import io.flutter.plugin.common.EventChannel.EventSink
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region


const val KEY_SCANNER = "key_scanner"
const val KEY_ACTION = "key_action"
const val KEY_REGIONS = "key_regions"
const val ACTION_START_RANGING = "action_start_ranging"
const val ACTION_STOP_RANGING = "action_stop_ranging"
const val ACTION_START_MONITORING = "action_start_monitoring"
const val ACTION_STOP_MONITORING = "action_stop_monitoring"
const val ACTION_START = "action_start"
const val ACTION_STOP = "action_stop"

const val BEACON_NOTIFICATION_ID = 1234

class RangingBeaconService : Service() {

    private var scanner: FlutterBeaconScanner? = null;
    private val plugin: FlutterBeaconPlugin?
        get() = scanner?.plugin
    private val beaconManager: BeaconManager?
        get() = plugin?.beaconManager
    private val beaconChannel = "BeaconChannel"
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var eventSinkRanging: EventSink?
        get() = scanner?.eventSinkRanging
        set(value) {
            scanner?.eventSinkRanging = value
        }
    private var eventSinkMonitoring: EventSink?
        get() = scanner?.eventSinkMonitoring
        set(value) {
            scanner?.eventSinkMonitoring = value
        }

    private var regionRanging = mutableListOf<Region>()
    private var regionMonitoring = mutableListOf<Region>()

    override fun onBind(p0: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.getStringExtra(KEY_ACTION)) {
            ACTION_START -> initService(intent)
            ACTION_STOP -> stopBeacons()
            ACTION_START_RANGING -> startRangingAction(intent)
            ACTION_STOP_RANGING -> stopRanging()
            ACTION_START_MONITORING -> startMonitoringAction(intent)
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_NOT_STICKY
    }

    private fun stopBeacons() {
        if (beaconManager?.isBound(beaconConsumer) == true) {
            beaconManager!!.unbind(beaconConsumer)
        }
        stopForeground(true)
    }


    private fun startRangingAction(intent: Intent) {
        val ranges = intent.getParcelableArrayListExtra<Region>(KEY_REGIONS)!!
        regionRanging.clear()
        regionRanging.addAll(ranges)
        startRanging()
    }

    private fun startRanging() {
        if (beaconManager?.isBound(beaconConsumer) == false) {
            beaconManager?.bind(beaconConsumer)
        } else {
            if (regionRanging.isEmpty()) {
                Log.e("RANGING", "Region ranging is null or empty. Ranging not started.")
                return
            }
            try {
                beaconManager?.run {
                    removeAllRangeNotifiers()
                    addRangeNotifier(rangeNotifier)
                    for (region in regionRanging) {
                        startRangingBeaconsInRegion(region)
                    }
                }

            } catch (e: RemoteException) {
                eventSinkRanging?.error("Beacon", e.localizedMessage, null)
            }
        }
    }


    private fun stopRanging() {
        if (regionRanging.isNotEmpty()) {
            try {
                for (region in regionRanging) {
                    beaconManager?.stopRangingBeaconsInRegion(region)
                }
                beaconManager?.removeRangeNotifier(rangeNotifier)
            } catch (ignored: RemoteException) {
            }
        }
        eventSinkRanging = null
    }

    private fun startMonitoringAction(intent: Intent) {
        val ranges = intent.getParcelableArrayListExtra<Region>(KEY_REGIONS)!!
        regionMonitoring.clear()
        regionMonitoring.addAll(ranges)
        startMonitoring()
    }

    private fun startMonitoring() {
        if (beaconManager?.isBound(beaconConsumer) == false) {
            beaconManager?.bind(beaconConsumer)
        } else {
            if (regionMonitoring.isEmpty()) {
                Log.e("MONITORING", "Region monitoring is null or empty. Monitoring not started.")
                return
            }
            try {
                beaconManager?.run {
                    removeAllMonitorNotifiers()
                    addMonitorNotifier(monitorNotifier)
                    for (region in regionMonitoring) {
                        startMonitoringBeaconsInRegion(region)
                    }
                }

            } catch (e: RemoteException) {
                eventSinkMonitoring?.error("Beacon", e.localizedMessage, null)
            }
        }
    }

    private fun stopMonitoring() {
        if (regionMonitoring.isNotEmpty()) {
            try {
                for (region in regionMonitoring) {
                    beaconManager?.stopMonitoringBeaconsInRegion(region)
                }
                beaconManager?.removeMonitorNotifier(monitorNotifier)
            } catch (ignored: RemoteException) {
            }
        }
        eventSinkMonitoring = null
    }

    private val rangeNotifier = RangeNotifier { collection, region ->
        if (eventSinkRanging != null) {
            val map: MutableMap<String, Any> = HashMap()
            map["region"] = FlutterBeaconUtils.regionToMap(region)
            map["beacons"] = FlutterBeaconUtils.beaconsToArray(ArrayList(collection))
            handler.post {
                eventSinkRanging?.success(map)
            }
        }
    }

    private fun initService(intent: Intent) {
        createNotificationChannel()
        val pendingIntent: PendingIntent =
            packageManager.getLaunchIntentForPackage(packageName)!!.let { notificationIntent ->
                PendingIntent.getService(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }
        val notification: Notification = NotificationCompat.Builder(this, beaconChannel)
            .setContentTitle("Beacon")
            .setContentText("Detecting Beacons")
            .setSmallIcon(drawable.ic_menu_week)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(BEACON_NOTIFICATION_ID, notification)
        intent.getLongExtra(KEY_SCANNER, -1).run {
            scanner = scannerMap[this]?.get()
        }
        if (beaconManager?.isBound(beaconConsumer) == false) {
            beaconManager!!.bind(beaconConsumer)
        }
    }

    private fun createNotificationChannel() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                beaconChannel,
                "Beacon Detections",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private val monitorNotifier: MonitorNotifier = object : MonitorNotifier {
        override fun didEnterRegion(region: Region) {
            eventSinkMonitoring?.run {
                val map: MutableMap<String, Any> = HashMap()
                map["event"] = "didEnterRegion"
                map["region"] = FlutterBeaconUtils.regionToMap(region)
                handler.post { success(map) }
            }
        }

        override fun didExitRegion(region: Region) {
            eventSinkMonitoring?.run {
                val map: MutableMap<String, Any> = HashMap()
                map["event"] = "didExitRegion"
                map["region"] = FlutterBeaconUtils.regionToMap(region)
                handler.post { success(map) }
            }
        }

        override fun didDetermineStateForRegion(state: Int, region: Region) {
            eventSinkMonitoring?.run {
                val map: MutableMap<String, Any> = HashMap()
                map["event"] = "didDetermineStateForRegion"
                map["state"] = FlutterBeaconUtils.parseState(state)
                map["region"] = FlutterBeaconUtils.regionToMap(region)
                handler.post { success(map) }
            }
        }
    }

    val beaconConsumer = object : BeaconConsumer {
        override fun onBeaconServiceConnect() {
            if (plugin?.flutterResult != null) {
                plugin?.flutterResult?.success(true)
                plugin?.flutterResult = null
            } else {
                startRanging()
                startMonitoring()
            }
        }

        override fun getApplicationContext(): Context = this@RangingBeaconService.applicationContext

        override fun unbindService(serviceConnection: ServiceConnection) {
            this@RangingBeaconService.unbindService(serviceConnection)
        }

        override fun bindService(
            intent: Intent,
            serviceConnection: ServiceConnection,
            i: Int
        ): Boolean {
            return this@RangingBeaconService.bindService(intent, serviceConnection, i)
        }
    }

    companion object {
        var instance: RangingBeaconService? = null
    }
}