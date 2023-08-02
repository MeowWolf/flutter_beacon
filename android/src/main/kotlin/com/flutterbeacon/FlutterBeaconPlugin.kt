package com.flutterbeacon

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.RemoteException
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flutterbeacon.RangingBeaconService.Companion.instance
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser

class FlutterBeaconPlugin : FlutterPlugin, ActivityAware, MethodCallHandler,
    RequestPermissionsResultListener, ActivityResultListener {
    private var flutterPluginBinding: FlutterPluginBinding? = null
    private var activityPluginBinding: ActivityPluginBinding? = null
    private var beaconScanner: FlutterBeaconScanner? = null
    private var beaconBroadcast: FlutterBeaconBroadcast? = null
    private var platform: FlutterPlatform? = null
    var beaconManager: BeaconManager? = null
        private set
    var flutterResult: Result? = null
    private var flutterResultBluetooth: Result? = null
    private var eventSinkLocationAuthorizationStatus: EventSink? = null
    private var channel: MethodChannel? = null
    private var eventChannel: EventChannel? = null
    private var eventChannelMonitoring: EventChannel? = null
    private var eventChannelBluetoothState: EventChannel? = null
    private var eventChannelAuthorizationStatus: EventChannel? = null
    private var context: Context? = null

    private val beaconEventChannel = BeaconEventChannel()
    private var flutterBeaconNotifyChannel: EventChannel? = null
    private var broadcastManager: LocalBroadcastManager? = null

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        flutterPluginBinding = binding
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        flutterPluginBinding = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityPluginBinding = binding
        setupChannels(flutterPluginBinding!!.binaryMessenger, binding.activity)
        setupMiscChannels(
            flutterPluginBinding!!.binaryMessenger,
            binding.activity.applicationContext
        )
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        teardownChannels()
    }

    private fun setupChannels(messenger: BinaryMessenger, activity: Activity?) {
        if (activityPluginBinding != null) {
            activityPluginBinding!!.addActivityResultListener(this)
            activityPluginBinding!!.addRequestPermissionsResultListener(this)
        }
        beaconManager = BeaconManager.getInstanceForApplication(activity!!.applicationContext)
        if (!beaconManager!!.beaconParsers.contains(iBeaconLayout)) {
            beaconManager!!.beaconParsers.clear()
            beaconManager!!.beaconParsers.add(iBeaconLayout)
        }
        platform = FlutterPlatform(activity)
        beaconScanner = FlutterBeaconScanner(this, activity.applicationContext)
        beaconBroadcast = FlutterBeaconBroadcast(
            activity.applicationContext,
            iBeaconLayout
        )
        channel = MethodChannel(messenger, "flutter_beacon")
        channel!!.setMethodCallHandler(this)
        eventChannel = EventChannel(messenger, "flutter_beacon_event")
        eventChannel!!.setStreamHandler(beaconScanner!!.rangingStreamHandler)
        eventChannelMonitoring = EventChannel(messenger, "flutter_beacon_event_monitoring")
        eventChannelMonitoring!!.setStreamHandler(beaconScanner!!.monitoringStreamHandler)
        eventChannelBluetoothState = EventChannel(messenger, "flutter_bluetooth_state_changed")
        eventChannelBluetoothState!!.setStreamHandler(
            FlutterBluetoothStateReceiver(
                activity
            )
        )
        eventChannelAuthorizationStatus =
            EventChannel(messenger, "flutter_authorization_status_changed")
        eventChannelAuthorizationStatus!!.setStreamHandler(locationAuthorizationStatusStreamHandler)
    }

    private fun setupMiscChannels(messenger: BinaryMessenger, context: Context) {
        with(EventChannel(messenger, beaconEventChannel.name)) {
            setStreamHandler(beaconEventChannel)
            flutterBeaconNotifyChannel = this
        }
        broadcastManager = LocalBroadcastManager.getInstance(context)
        broadcastManager?.registerReceiver(
            beaconEventChannel,
            IntentFilter(beaconEventChannel.action)
        )
    }

    private fun teardownChannels() {
        if (activityPluginBinding != null) {
            activityPluginBinding!!.removeActivityResultListener(this)
            activityPluginBinding!!.removeRequestPermissionsResultListener(this)
        }
        platform = null
        beaconBroadcast = null
        channel?.setMethodCallHandler(null)
        eventChannel?.setStreamHandler(null)
        flutterBeaconNotifyChannel?.setStreamHandler(null)
        eventChannelMonitoring?.setStreamHandler(null)
        eventChannelBluetoothState?.setStreamHandler(null)
        eventChannelAuthorizationStatus?.setStreamHandler(null)
        channel = null
        eventChannel = null
        eventChannelMonitoring = null
        eventChannelBluetoothState = null
        eventChannelAuthorizationStatus = null
        activityPluginBinding = null
        flutterBeaconNotifyChannel = null
        broadcastManager?.unregisterReceiver(
            beaconEventChannel,
        )
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "initialize") {
            startService()
            val service = instance
            if (beaconManager != null && service != null && !beaconManager!!.isBound(service.beaconConsumer)) {
                flutterResult = result
                beaconManager!!.bind(service.beaconConsumer)
                return
            }
            result.success(true)
            return
        }
        if (call.method == "initializeAndCheck") {
            initializeAndCheck(result)
            return
        }
        if (call.method == "setScanPeriod") {
            val scanPeriod = call.argument<Int>("scanPeriod")!!
            beaconManager!!.foregroundScanPeriod = scanPeriod.toLong()
            try {
                beaconManager!!.updateScanPeriods()
                result.success(true)
            } catch (e: RemoteException) {
                result.success(false)
            }
        }
        if (call.method == "setBetweenScanPeriod") {
            val betweenScanPeriod = call.argument<Int>("betweenScanPeriod")!!
            beaconManager!!.foregroundBetweenScanPeriod = betweenScanPeriod.toLong()
            try {
                beaconManager!!.updateScanPeriods()
                result.success(true)
            } catch (e: RemoteException) {
                result.success(false)
            }
        }
        if (call.method == "setLocationAuthorizationTypeDefault") {
            // Android does not have the concept of "requestWhenInUse" and "requestAlways" like iOS does,
            // so this method does nothing.
            // (Well, in Android API 29 and higher, there is an "ACCESS_BACKGROUND_LOCATION" option,
            //  which could perhaps be appropriate to add here as an improvement.)
            result.success(true)
            return
        }
        if (call.method == "authorizationStatus") {
            result.success(if (platform!!.checkLocationServicesPermission()) "ALLOWED" else "NOT_DETERMINED")
            return
        }
        if (call.method == "checkLocationServicesIfEnabled") {
            result.success(platform!!.checkLocationServicesIfEnabled())
            return
        }
        if (call.method == "bluetoothState") {
            try {
                val flag = platform!!.checkBluetoothIfEnabled()
                result.success(if (flag) "STATE_ON" else "STATE_OFF")
                return
            } catch (ignored: RuntimeException) {
            }
            result.success("STATE_UNSUPPORTED")
            return
        }
        if (call.method == "requestAuthorization") {
            if (!platform!!.checkLocationServicesPermission()) {
                flutterResult = result
                platform!!.requestAuthorization()
                return
            }

            // Here, location services permission is granted.
            //
            // It's possible location permission was granted without going through
            // our onRequestPermissionsResult() - for example if a different flutter plugin
            // also requested location permissions, we could end up here with
            // checkLocationServicesPermission() returning true before we ever called requestAuthorization().
            //
            // In that case, we'll never get a notification posted to eventSinkLocationAuthorizationStatus
            //
            // So we could could have flutter code calling requestAuthorization here and expecting to see
            // a change in eventSinkLocationAuthorizationStatus but never receiving it.
            //
            // Ensure an ALLOWED status (possibly duplicate) is posted back.
            if (eventSinkLocationAuthorizationStatus != null) {
                eventSinkLocationAuthorizationStatus!!.success("ALLOWED")
            }
            result.success(true)
            return
        }
        if (call.method == "openBluetoothSettings") {
            if (!platform!!.checkBluetoothIfEnabled()) {
                flutterResultBluetooth = result
                platform!!.openBluetoothSettings()
                return
            }
            result.success(true)
            return
        }
        if (call.method == "openLocationSettings") {
            platform!!.openLocationSettings()
            result.success(true)
            return
        }
        if (call.method == "openApplicationSettings") {
            result.notImplemented()
            return
        }
        if (call.method == "close") {
            if (beaconManager != null) {
                beaconScanner!!.stopRanging()
                beaconManager!!.removeAllRangeNotifiers()
                beaconScanner!!.stopMonitoring()
                beaconManager!!.removeAllMonitorNotifiers()
                val service = instance
                if (service != null && beaconManager!!.isBound(service.beaconConsumer)) {
                    beaconManager!!.unbind(service.beaconConsumer)
                }
                val intent = Intent(context, RangingBeaconService::class.java)
                intent.putExtra(KEY_ACTION, ACTION_STOP)
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    context!!.startForegroundService(intent)
                } else {
                    context!!.startService(intent)
                }
            }
            result.success(true)
            return
        }
        if (call.method == "startBroadcast") {
            beaconBroadcast!!.startBroadcast(call.arguments, result)
            return
        }
        if (call.method == "stopBroadcast") {
            beaconBroadcast!!.stopBroadcast(result)
            return
        }
        if (call.method == "isBroadcasting") {
            beaconBroadcast!!.isBroadcasting(result)
            return
        }
        if (call.method == "isBroadcastSupported") {
            result.success(platform!!.isBroadcastSupported)
            return
        }
        result.notImplemented()
    }

    private fun startService() {
        val intent = Intent(context, RangingBeaconService::class.java)
        intent.putExtra(KEY_ACTION, ACTION_START)
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            context!!.startForegroundService(intent)
        } else {
            context!!.startService(intent)
        }
    }

    private fun initializeAndCheck(result: Result?) {
        if (platform!!.checkLocationServicesPermission()
            && platform!!.checkBluetoothIfEnabled()
            && platform!!.checkLocationServicesIfEnabled()
        ) {
            if (result != null) {
                result.success(true)
                return
            }
        }
        flutterResult = result
        if (!platform!!.checkBluetoothIfEnabled()) {
            platform!!.openBluetoothSettings()
            return
        }
        if (!platform!!.checkLocationServicesPermission()) {
            platform!!.requestAuthorization()
            return
        }
        if (!platform!!.checkLocationServicesIfEnabled()) {
            platform!!.openLocationSettings()
            return
        }
        val service = instance
        startService()
        if (beaconManager != null && service != null && !beaconManager!!.isBound(service.beaconConsumer)) {
            if (result != null) {
                flutterResult = result
            }
            beaconManager!!.bind(service.beaconConsumer)
            return
        }
        result?.success(true)
    }

    private val locationAuthorizationStatusStreamHandler: StreamHandler = object : StreamHandler {
        override fun onListen(arguments: Any, events: EventSink) {
            eventSinkLocationAuthorizationStatus = events
        }

        override fun onCancel(arguments: Any) {
            eventSinkLocationAuthorizationStatus = null
        }
    }

    // region ACTIVITY CALLBACK
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode != REQUEST_CODE_LOCATION) {
            return false
        }
        var locationServiceAllowed = false
        if (permissions.size > 0 && grantResults.size > 0) {
            val permission = permissions[0]
            if (!platform!!.shouldShowRequestPermissionRationale(permission)) {
                val grantResult = grantResults[0]
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    //allowed
                    locationServiceAllowed = true
                }
                if (eventSinkLocationAuthorizationStatus != null) {
                    // shouldShowRequestPermissionRationale = false, so if access wasn't granted, the user clicked DENY and checked DON'T SHOW AGAIN
                    eventSinkLocationAuthorizationStatus!!.success(if (locationServiceAllowed) "ALLOWED" else "DENIED")
                }
            } else {
                // shouldShowRequestPermissionRationale = true, so the user has clicked DENY but not DON'T SHOW AGAIN, we can possibly prompt again
                if (eventSinkLocationAuthorizationStatus != null) {
                    eventSinkLocationAuthorizationStatus!!.success("NOT_DETERMINED")
                }
            }
        } else {
            // Permission request was cancelled (another requestPermission active, other interruptions), we can possibly prompt again
            if (eventSinkLocationAuthorizationStatus != null) {
                eventSinkLocationAuthorizationStatus!!.success("NOT_DETERMINED")
            }
        }
        if (flutterResult != null) {
            if (locationServiceAllowed) {
                flutterResult!!.success(true)
            } else {
                flutterResult!!.error("Beacon", "location services not allowed", null)
            }
            flutterResult = null
        }
        return locationServiceAllowed
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        val bluetoothEnabled =
            requestCode == REQUEST_CODE_BLUETOOTH && resultCode == Activity.RESULT_OK
        if (bluetoothEnabled) {
            if (!platform!!.checkLocationServicesPermission()) {
                platform!!.requestAuthorization()
            } else {
                if (flutterResultBluetooth != null) {
                    flutterResultBluetooth!!.success(true)
                    flutterResultBluetooth = null
                } else if (flutterResult != null) {
                    flutterResult!!.success(true)
                    flutterResult = null
                }
            }
        } else {
            if (flutterResultBluetooth != null) {
                flutterResultBluetooth!!.error("Beacon", "bluetooth disabled", null)
                flutterResultBluetooth = null
            } else if (flutterResult != null) {
                flutterResult!!.error("Beacon", "bluetooth disabled", null)
                flutterResult = null
            }
        }
        return bluetoothEnabled
    } // endregion

    companion object {
        private val iBeaconLayout = BeaconParser()
            .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        const val REQUEST_CODE_LOCATION = 1234
        const val REQUEST_CODE_BLUETOOTH = 5678

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = FlutterBeaconPlugin()
            instance.setupChannels(registrar.messenger(), registrar.activity())
            instance.context = registrar.activity()!!.applicationContext
            registrar.addActivityResultListener(instance)
            registrar.addRequestPermissionsResultListener(instance)
        }
    }
}