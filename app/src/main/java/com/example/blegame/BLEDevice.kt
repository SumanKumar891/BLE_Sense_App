package com.example.blegame

data class BLEDevice(
    var name: String?,
    var address: String?,
    var rssi: String = 0.toString(),
    var lastSeen: Long = System.currentTimeMillis(),
    var isFound: Boolean = false,
)

data class SHT40Reading(
    val timestamp: Long,
    val deviceId: String,
    val temperature: String,
    val humidity: String
)

data class LuxSensorReading(
    val timestamp: Long,
    val deviceId: String,
    val lux: Float
)

data class LIS2DHReading(
    val timestamp: Long,
    val deviceId: String,
    val x: String,
    val y: String,
    val z: String
)

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



