package com.flutterbeacon

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.util.Log
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter
import java.util.Map
import io.flutter.plugin.common.MethodChannel

internal class FlutterBeaconBroadcast(context: Context?, iBeaconLayout: BeaconParser?) {
    private val beaconTransmitter: BeaconTransmitter

    init {
        beaconTransmitter = BeaconTransmitter(context, iBeaconLayout)
    }

    fun isBroadcasting(@NonNull result: MethodChannel.Result) {
        result.success(beaconTransmitter.isStarted())
    }

    fun stopBroadcast(@NonNull result: MethodChannel.Result) {
        beaconTransmitter.stopAdvertising()
        result.success(true)
    }

    @SuppressWarnings("rawtypes")
    fun startBroadcast(arguments: Object, @NonNull result: MethodChannel.Result) {
        if (arguments !is Map) {
            result.error("Broadcast", "Invalid parameter", null)
            return
        }
        val map: Map = arguments
        val beacon: Beacon = FlutterBeaconUtils.beaconFromMap(map)
        val advertisingMode: Object = map.get("advertisingMode")
        if (advertisingMode is Integer) {
            beaconTransmitter.setAdvertiseMode(advertisingMode as Integer)
        }
        val advertisingTxPowerLevel: Object = map.get("advertisingTxPowerLevel")
        if (advertisingTxPowerLevel is Integer) {
            beaconTransmitter.setAdvertiseTxPowerLevel(advertisingTxPowerLevel as Integer)
        }
        beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
            @Override
            fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "Start broadcasting = $beacon")
                result.success(true)
            }

            @Override
            fun onStartFailure(errorCode: Int) {
                var error = "FEATURE_UNSUPPORTED"
                if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                    error = "DATA_TOO_LARGE"
                } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                    error = "TOO_MANY_ADVERTISERS"
                } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                    error = "ALREADY_STARTED"
                } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                    error = "INTERNAL_ERROR"
                }
                Log.e(TAG, error)
                result.error("Broadcast", error, null)
            }
        })
    }

    companion object {
        private val TAG: String = FlutterBeaconBroadcast::class.java.getSimpleName()
    }
}