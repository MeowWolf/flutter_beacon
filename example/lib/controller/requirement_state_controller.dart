import 'package:flutter/foundation.dart';
import 'package:flutter_beacon/flutter_beacon.dart';
import 'package:get/get.dart';

class RequirementStateController extends GetxController {
  var bluetoothState = BluetoothState.stateOff.obs;
  var authorizationStatus = AuthorizationStatus.notDetermined.obs;
  var backgroundState = false.obs;
  var locationService = false.obs;
  var detectedBeaconCount = 0.obs;
  var _startBroadcasting = false.obs;
  var _startScanning = false.obs;
  var _pauseScanning = false.obs;

  bool get bluetoothEnabled => bluetoothState.value == BluetoothState.stateOn;
  bool get authorizationStatusOk =>
      authorizationStatus.value == AuthorizationStatus.allowed ||
      authorizationStatus.value == AuthorizationStatus.always;
  bool get locationServiceEnabled => locationService.value;
  bool get isAppInBackground => backgroundState.value;

  updateBluetoothState(BluetoothState state) {
    bluetoothState.value = state;
  }

  updateAuthorizationStatus(AuthorizationStatus status) {
    authorizationStatus.value = status;
  }

  updateLocationService(bool flag) {
    locationService.value = flag;
  }

  updateDetectedBeacon(int count) {
    detectedBeaconCount.value = count;
  }

  updateAppState(bool isInBackground) {
    backgroundState.value = isInBackground;
  }

  startBroadcasting() {
    _startBroadcasting.value = true;
  }

  stopBroadcasting() {
    _startBroadcasting.value = false;
  }

  startScanning() {
    _startScanning.value = true;
    _pauseScanning.value = false;
  }

  pauseScanning() {
    _startScanning.value = false;
    _pauseScanning.value = true;
  }

  Stream<bool> get startBroadcastStream {
    return _startBroadcasting.stream;
  }

  Stream<bool> get startStream {
    return _startScanning.stream;
  }

  Stream<bool> get pauseStream {
    return _pauseScanning.stream;
  }

  bool hasNewBeaconDetected(int count) {
    return detectedBeaconCount.value < count;
  }
}
