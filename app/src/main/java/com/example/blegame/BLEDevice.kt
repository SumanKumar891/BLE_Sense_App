package com.example.blegame


// Data Class for displaying Data in Devices List

data class BLEDevice(
    var name: String?,
    var address: String?,
    var rssi: String = 0.toString(),
    var lastSeen: Long = System.currentTimeMillis(),
    var isFound: Boolean = false,
)

// Data class for SHT40

data class SHT40Reading(
    val timestamp: Long,
    val deviceId: String,
    val temperature: String,
    val humidity: String
)

// Data class for Lux

data class LuxSensorReading(
    val timestamp: Long,
    val deviceId: String,
    val lux: Float
)

// Data class for LIS2DH

data class LIS2DHReading(
    val timestamp: Long,
    val deviceId: String,
    val x: String,
    val y: String,
    val z: String
)

// Data class for Soil Sensor

data class SoilSensorReading(
    val timestamp: Long,
    val deviceId: String,
    val nitrogen: String,
    val phosphorus: String,
    val potassium: String,
    val moisture: String,
    val temperature: String,
    val electricalConductivity: String,
    val pH: String
)

// Data class for Weather Sensor

data class WeatherReading(
    val timestamp: Long,
    val deviceId1: String,
    val temperature1: String,
    val humidity1: String,
    val pressure1: String,
    val dewPointTemperature1: String,
    val deviceId2: String,
    val temperature2: String,
    val humidity2: String,
    val pressure2: String,
    val dewPointTemperature2: String
)



