package com.example.blegame

object WeatherDataParser {
    fun parseWeatherData(data: ByteArray): String {
        if (data.size < 20) {
            return "Insufficient data. Expected at least 20 bytes but received ${data.size}."
        }
        val builder = StringBuilder()
        // Display raw BLE data as hex
        builder.append("Raw BLE Data: ")
        builder.append(data.joinToString(" ") { byte -> String.format("%02X", byte) })
            .append("\n\n")
        // DeviceId 1
        val deviceId1 = data[0].toUByte().toString()
        // Temperature 1 (bytes 1 and 2) with a decimal between
        val temperature1 = "${data[1].toUByte()}.${data[2].toUByte()}°C"
        // Humidity 1 (bytes 3 and 4) with a decimal between
        val humidity1 = "${data[3].toUByte()}.${data[4].toUByte()}%"
        // Pressure 1 (bytes 5 and 6) with a decimal between
        val pressure1 = "${data[5].toUByte()}.${data[6].toUByte()} hPa"
        // Dew Point Temperature 1 (bytes 7 and 8) with a decimal between
        val dewPointTemperature1 = "${data[7].toUByte()}.${data[8].toUByte()}°C"
        // Skip bytes 9 and 10, DeviceId 2 starts at byte 11
        val deviceId2 = data[11].toUByte().toString()
        // Temperature 2 (bytes 12 and 13) with a decimal between
        val temperature2 = "${data[12].toUByte()}.${data[13].toUByte()}°C"
        // Humidity 2 (bytes 14 and 15) with a decimal between
        val humidity2 = "${data[14].toUByte()}.${data[15].toUByte()}%"
        // Pressure 2 (bytes 16 and 17) with a decimal between
        val pressure2 = "${data[16].toUByte()}.${data[17].toUByte()} hPa"
        // Dew Point Temperature 2 (bytes 18 and 19) with a decimal between
        val dewPointTemperature2 = "${data[18].toUByte()}.${data[19].toUByte()}°C"
        builder.append("Parsed Sensor Data:\n")
        builder.append("DeviceId 1: $deviceId1\n")
        builder.append("Temperature 1: $temperature1\n")
        builder.append("Humidity 1: $humidity1\n")
        builder.append("Pressure 1: $pressure1\n")
        builder.append("Dew Point Temperature 1: $dewPointTemperature1\n")
        builder.append("\n")
        builder.append("DeviceId 2: $deviceId2\n")
        builder.append("Temperature 2: $temperature2\n")
        builder.append("Humidity 2: $humidity2\n")
        builder.append("Pressure 2: $pressure2\n")
        builder.append("Dew Point Temperature 2: $dewPointTemperature2")
        return builder.toString()
    }
}
