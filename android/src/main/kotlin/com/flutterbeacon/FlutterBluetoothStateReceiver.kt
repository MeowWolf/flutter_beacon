package com.flutterbeacon

import android.annotation.SuppressLint

internal class FlutterBluetoothStateReceiver(context: Context) : BroadcastReceiver(),
    StreamHandler {
    private val context: Context
    private var eventSink: EventChannel.EventSink? = null

    init {
        this.context = context
    }

    @Override
    fun onReceive(context: Context?, intent: Intent) {
        if (eventSink == null) return
        val action: String = intent.getAction()
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            val state: Int =
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            sendState(state)
        }
    }

    private fun sendState(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> eventSink.success("STATE_OFF")
            BluetoothAdapter.STATE_TURNING_OFF -> eventSink.success("STATE_TURNING_OFF")
            BluetoothAdapter.STATE_ON -> eventSink.success("STATE_ON")
            BluetoothAdapter.STATE_TURNING_ON -> eventSink.success("STATE_TURNING_ON")
            else -> eventSink.error("BLUETOOTH_STATE", "invalid bluetooth adapter state", null)
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    fun onListen(o: Object?, eventSink: EventChannel.EventSink?) {
        var state: Int = BluetoothAdapter.STATE_OFF
        val bluetoothManager: BluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager != null) {
            val adapter: BluetoothAdapter = bluetoothManager.getAdapter()
            if (adapter != null) {
                state = adapter.getState()
            }
        }
        this.eventSink = eventSink
        sendState(state)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(this, filter)
    }

    @Override
    fun onCancel(o: Object?) {
        context.unregisterReceiver(this)
    }
}