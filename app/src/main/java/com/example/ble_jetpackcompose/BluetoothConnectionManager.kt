import android.bluetooth.BluetoothSocket
import android.util.Log

object BluetoothConnectionManager {
    var bluetoothSocket: BluetoothSocket? = null

    fun disconnect() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Error closing socket: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }
}