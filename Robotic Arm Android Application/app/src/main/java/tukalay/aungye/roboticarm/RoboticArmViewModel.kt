package tukalay.aungye.roboticarm

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

data class SettingsState(
    val username: String = "",
    val password: String = "",
    val l1: String = "60.0",
    val l2: String = "60.0",
    val l3: String = "60.0",
    val l4: String = "60.0",
    val v: String = "60.0",
    val h1: String = "60.0",
    val h2: String = "60.0",
    val a1: String = "60.0"
)

class RoboticArmViewModel(app: Application) : AndroidViewModel(app) {

    val degrees = mutableStateListOf(90f, 90f, 90f, 90f, 90f, 90f)
    var isConnected by mutableStateOf(false)
        private set
    var connectedName by mutableStateOf("")
        private set
    var logText by mutableStateOf("")
        private set

    private var btSocket: BluetoothSocket? = null
    private var btJob: Job? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val prefs = app.getSharedPreferences("RoboticArm", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains("firstUser")) {
            prefs.edit().apply {
                putBoolean("firstUser", true)
                putString("username", "user")
                putString("password", "password")
                putFloat("arm1", 60f); putFloat("arm2", 60f)
                putFloat("arm3", 60f); putFloat("gripper", 60f)
                putFloat("high1", 60f); putFloat("high2", 60f)
                putFloat("phase", 60f); putFloat("newLength", 60f)
                apply()
            }
        }
    }

    fun loadSettings(): SettingsState = SettingsState(
        username = prefs.getString("username", "") ?: "",
        password = prefs.getString("password", "") ?: "",
        l1 = prefs.getFloat("arm1", 60f).toString(),
        l2 = prefs.getFloat("arm2", 60f).toString(),
        l3 = prefs.getFloat("arm3", 60f).toString(),
        l4 = prefs.getFloat("gripper", 60f).toString(),
        v = prefs.getFloat("newLength", 60f).toString(),
        h1 = prefs.getFloat("high1", 60f).toString(),
        h2 = prefs.getFloat("high2", 60f).toString(),
        a1 = prefs.getFloat("phase", 60f).toString()
    )

    fun saveSettings(s: SettingsState) {
        prefs.edit().apply {
            putString("username", s.username)
            putString("password", s.password)
            putFloat("arm1", s.l1.toFloatOrNull() ?: 60f)
            putFloat("arm2", s.l2.toFloatOrNull() ?: 60f)
            putFloat("arm3", s.l3.toFloatOrNull() ?: 60f)
            putFloat("gripper", s.l4.toFloatOrNull() ?: 60f)
            putFloat("newLength", s.v.toFloatOrNull() ?: 60f)
            putFloat("high1", s.h1.toFloatOrNull() ?: 60f)
            putFloat("high2", s.h2.toFloatOrNull() ?: 60f)
            putFloat("phase", s.a1.toFloatOrNull() ?: 60f)
            apply()
        }
    }

    fun pairedDevices(adapter: BluetoothAdapter): List<Pair<String, String>> =
        adapter.bondedDevices?.map { Pair(it.name ?: "Unknown", it.address) } ?: emptyList()

    fun connectToDevice(adapter: BluetoothAdapter, address: String, name: String) {
        btJob?.cancel()
        connectedName = name
        appendLog("Connecting to $address…")
        isConnected = false

        btJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val device = adapter.getRemoteDevice(address)
                adapter.cancelDiscovery()
                btSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                btSocket?.connect()

                withContext(Dispatchers.Main) {
                    isConnected = true
                    appendLog("Connected to $connectedName")
                }

                val reader = BufferedReader(InputStreamReader(btSocket?.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    withContext(Dispatchers.Main) { appendLog(line) }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    isConnected = false
                    appendLog("Connection error: ${e.message}")
                }
            }
        }
    }

    fun disconnect() {
        btJob?.cancel()
        btSocket?.close()
        btSocket = null
        isConnected = false
        appendLog("Disconnected")
    }

    fun sendDegrees() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val packet = byteArrayOf(
                    'N'.code.toByte(),
                    degrees[0].toInt().toByte(),
                    degrees[1].toInt().toByte(),
                    degrees[2].toInt().toByte(),
                    degrees[3].toInt().toByte(),
                    degrees[4].toInt().toByte(),
                    degrees[5].toInt().toByte(),
                    '0'.code.toByte()
                )
                btSocket?.outputStream?.write(packet)
            } catch (e: IOException) {
                withContext(Dispatchers.Main) { appendLog("Send error: ${e.message}") }
            }
        }
    }

    private fun appendLog(message: String) {
        logText = "› $message\n$logText"
        if (logText.length > 2000) logText = logText.take(2000)
    }

    override fun onCleared() {
        super.onCleared()
        btSocket?.close()
    }
}
