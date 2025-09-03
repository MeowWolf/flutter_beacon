package com.flutterbeacon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.altbeacon.beacon.BeaconTransmitter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class FlutterPlatform {
  private final WeakReference<Activity> activityWeakReference;

  FlutterPlatform(Activity activity) {
    activityWeakReference = new WeakReference<>(activity);
  }

  private Activity getActivity() {
    return activityWeakReference.get();
  }

  void openLocationSettings() {
    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    getActivity().startActivity(intent);
  }

  void openBluetoothSettings() {
    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    getActivity().startActivityForResult(intent, FlutterBeaconPlugin.REQUEST_CODE_BLUETOOTH);
  }

  void requestAuthorization() {
    List<String> permissions = new ArrayList<>();
    
    // Add location permissions
    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
    
    // Add Bluetooth permissions for Android 12+ (API 31+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(Manifest.permission.BLUETOOTH_SCAN);
      permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
      permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
    } else {
      // For older Android versions, add legacy Bluetooth permissions
      permissions.add(Manifest.permission.BLUETOOTH);
      permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
    }
    
    ActivityCompat.requestPermissions(getActivity(), 
        permissions.toArray(new String[0]), 
        FlutterBeaconPlugin.REQUEST_CODE_LOCATION);
  }

  boolean checkLocationServicesPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return ContextCompat.checkSelfPermission(getActivity(),
          Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    return true;
  }

  boolean checkBluetoothPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // Check Android 12+ Bluetooth permissions
      return ContextCompat.checkSelfPermission(getActivity(), 
          Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(getActivity(), 
          Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(getActivity(), 
          Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    } else {
      // Check legacy Bluetooth permissions for older Android versions
      return ContextCompat.checkSelfPermission(getActivity(), 
          Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(getActivity(), 
          Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
    }
  }

  boolean checkAllPermissions() {
    return checkLocationServicesPermission() && checkBluetoothPermissions();
  }

  boolean checkLocationServicesIfEnabled() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
      return locationManager != null && locationManager.isLocationEnabled();
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int mode = Settings.Secure.getInt(getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE,
          Settings.Secure.LOCATION_MODE_OFF);
      return (mode != Settings.Secure.LOCATION_MODE_OFF);
    }

    return true;
  }

  @SuppressLint("MissingPermission")
  boolean checkBluetoothIfEnabled() {
    BluetoothManager bluetoothManager = (BluetoothManager)
        getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
    if (bluetoothManager == null) {
      throw new RuntimeException("No bluetooth service");
    }

    BluetoothAdapter adapter = bluetoothManager.getAdapter();

    return (adapter != null) && (adapter.isEnabled());
  }

  boolean isBroadcastSupported() {
    return BeaconTransmitter.checkTransmissionSupported(getActivity()) == 0;
  }

  boolean shouldShowRequestPermissionRationale(String permission) {
    return ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission);
  }
}
