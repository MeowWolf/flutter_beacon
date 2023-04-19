package com.flutterbeacon

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.altbeacon.beacon.BeaconTransmitter
import java.lang.ref.WeakReference

internal class FlutterPlatform(activity: Activity?) {
    private val activityWeakReference: WeakReference<Activity>

    init {
        activityWeakReference = WeakReference(activity)
    }

    private val activity: Activity
        private get() = activityWeakReference.get()

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    fun openBluetoothSettings() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(intent, FlutterBeaconPlugin.REQUEST_CODE_BLUETOOTH)
    }

    fun requestAuthorization() {
        ActivityCompat.requestPermissions(
            activity, arrayOf<String>(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), FlutterBeaconPlugin.REQUEST_CODE_LOCATION
        )
    }

    fun checkLocationServicesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) === PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun checkLocationServicesIfEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager: LocationManager =
                activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager != null && locationManager.isLocationEnabled()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val mode: Int = Settings.Secure.getInt(
                activity.getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            return mode != Settings.Secure.LOCATION_MODE_OFF
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun checkBluetoothIfEnabled(): Boolean {
        val bluetoothManager: BluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                ?: throw RuntimeException("No bluetooth service")
        val adapter: BluetoothAdapter = bluetoothManager.getAdapter()
        return adapter != null && adapter.isEnabled()
    }

    val isBroadcastSupported: Boolean
        get() = BeaconTransmitter.checkTransmissionSupported(activity) === 0

    fun shouldShowRequestPermissionRationale(permission: String?): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}