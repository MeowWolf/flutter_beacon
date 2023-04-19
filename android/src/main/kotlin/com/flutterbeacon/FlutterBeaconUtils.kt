package com.flutterbeacon

import android.util.Log
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map

internal object FlutterBeaconUtils {
    fun parseState(state: Int): String {
        return if (state == MonitorNotifier.INSIDE) "INSIDE" else if (state == MonitorNotifier.OUTSIDE) "OUTSIDE" else "UNKNOWN"
    }

    fun beaconsToArray(beacons: List<Beacon?>?): List<Map<String, Object>> {
        if (beacons == null) {
            return ArrayList()
        }
        val list: List<Map<String, Object>> = ArrayList()
        for (beacon in beacons) {
            val map: Map<String, Object> = beaconToMap(beacon)
            list.add(map)
        }
        return list
    }

    private fun beaconToMap(beacon: Beacon): Map<String, Object> {
        val map: Map<String, Object> = HashMap()
        map.put("proximityUUID", beacon.getId1().toString().toUpperCase())
        map.put("major", beacon.getId2().toInt())
        map.put("minor", beacon.getId3().toInt())
        map.put("rssi", beacon.getRssi())
        map.put("txPower", beacon.getTxPower())
        map.put("accuracy", String.format(Locale.US, "%.2f", beacon.getDistance()))
        map.put("macAddress", beacon.getBluetoothAddress())
        return map
    }

    fun regionToMap(region: Region): Map<String, Object> {
        val map: Map<String, Object> = HashMap()
        map.put("identifier", region.getUniqueId())
        if (region.getId1() != null) {
            map.put("proximityUUID", region.getId1().toString())
        }
        if (region.getId2() != null) {
            map.put("major", region.getId2().toInt())
        }
        if (region.getId3() != null) {
            map.put("minor", region.getId3().toInt())
        }
        return map
    }

    @SuppressWarnings("rawtypes")
    fun regionFromMap(map: Map): Region? {
        return try {
            var identifier = ""
            val identifiers: List<Identifier> = ArrayList()
            val objectIdentifier: Object = map.get("identifier")
            if (objectIdentifier is String) {
                identifier = objectIdentifier.toString()
            }
            val proximityUUID: Object = map.get("proximityUUID")
            if (proximityUUID is String) {
                identifiers.add(Identifier.parse(proximityUUID as String))
            }
            val major: Object = map.get("major")
            if (major is Integer) {
                identifiers.add(Identifier.fromInt(major as Integer))
            }
            val minor: Object = map.get("minor")
            if (minor is Integer) {
                identifiers.add(Identifier.fromInt(minor as Integer))
            }
            Region(identifier, identifiers)
        } catch (e: IllegalArgumentException) {
            Log.e("REGION", "Error : $e")
            null
        }
    }

    @SuppressWarnings("rawtypes")
    fun beaconFromMap(map: Map): Beacon {
        val builder: Beacon.Builder = Builder()
        val proximityUUID: Object = map.get("proximityUUID")
        if (proximityUUID is String) {
            builder.setId1(proximityUUID as String)
        }
        val major: Object = map.get("major")
        if (major is Integer) {
            builder.setId2(major.toString())
        }
        val minor: Object = map.get("minor")
        if (minor is Integer) {
            builder.setId3(minor.toString())
        }
        val txPower: Object = map.get("txPower")
        if (txPower is Integer) {
            builder.setTxPower(txPower as Integer)
        } else {
            builder.setTxPower(-59)
        }
        builder.setDataFields(Collections.singletonList(0L))
        builder.setManufacturer(0x004c)
        return builder.build()
    }
}