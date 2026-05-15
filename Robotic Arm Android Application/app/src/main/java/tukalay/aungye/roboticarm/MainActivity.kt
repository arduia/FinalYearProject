package tukalay.aungye.roboticarm

import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import tukalay.aungye.roboticarm.databinding.ActivityMainBinding
import tukalay.aungye.roboticarm.databinding.DialogChooseDeviceBinding
import tukalay.aungye.roboticarm.databinding.DialogSettingsBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private var btDevice: BluetoothDevice? = null
    private var btJob: Job? = null

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val degrees = FloatArray(6) { 90f }
    private val jointNames = arrayOf("Shoulder", "Base", "Elbow", "Wrist", "Wrist Spin", "Gripper")

    private var logText = ""
    private var connectedDeviceName = ""
    private lateinit var prefs: SharedPreferences

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initPrefs()
        initControls()
        checkBluetooth()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.devicemenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.choosebtdevice -> { showDeviceChooser(); true }
            R.id.showsettings -> { showSettings(); true }
            R.id.disconnect -> { disconnectDevice(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkBluetooth() {
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (!btAdapter!!.isEnabled) {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1)
        }
    }

    private fun initControls() {
        val sliders = listOf(
            binding.sliderShoulder, binding.sliderBase, binding.sliderElbow,
            binding.sliderWrist, binding.sliderWristSpin, binding.sliderGripper
        )
        val valueLabels = listOf(
            binding.tvShoulderValue, binding.tvBaseValue, binding.tvElbowValue,
            binding.tvWristValue, binding.tvWristSpinValue, binding.tvGripperValue
        )

        sliders.forEachIndexed { index, slider ->
            slider.valueFrom = 0f
            slider.valueTo = 180f
            slider.stepSize = 1f
            slider.value = 90f
            degrees[index] = 90f
            valueLabels[index].text = "90°"

            slider.addOnChangeListener { _, value, _ ->
                degrees[index] = value
                valueLabels[index].text = "${value.toInt()}°"
                if (btSocket != null) {
                    sendDegrees()
                    refreshStatusBar()
                } else {
                    setLog("Not connected — use menu to connect a device")
                }
            }
        }

        binding.btnSend.setOnClickListener {
            if (btSocket != null) {
                sendDegrees()
                appendLog("Manual send: ${degrees.map { it.toInt() }.joinToString(", ")}")
            } else {
                appendLog("Not connected — tap Connect in the menu")
            }
        }

        updateConnectionStatus(false)
    }

    private fun showDeviceChooser() {
        if (btAdapter?.isEnabled == false) {
            checkBluetooth()
            return
        }
        val dialogBinding = DialogChooseDeviceBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.setTitle("Choose Bluetooth Device")
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val deviceNames = mutableListOf<String>()
        val deviceAddresses = mutableListOf<String>()

        btAdapter?.bondedDevices?.forEach { device ->
            deviceNames.add("${device.name}\n${device.address}")
            deviceAddresses.add(device.address)
        }

        if (deviceNames.isEmpty()) {
            deviceNames.add("No paired devices found.\nPair a device in Bluetooth settings.")
        }

        dialogBinding.listDevices.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)

        dialogBinding.listDevices.setOnItemClickListener { _, _, position, _ ->
            if (position < deviceAddresses.size) {
                connectedDeviceName = deviceNames[position].lines().first()
                dialog.dismiss()
                connectToDevice(deviceAddresses[position])
            }
        }

        dialog.show()
    }

    private fun showSettings() {
        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.setTitle("Configuration")
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        with(dialogBinding) {
            etUsername.setText(prefs.getString("username", ""))
            etPassword.setText(prefs.getString("password", ""))
            etL1.setText(prefs.getFloat("arm1", 60f).toString())
            etL2.setText(prefs.getFloat("arm2", 60f).toString())
            etL3.setText(prefs.getFloat("arm3", 60f).toString())
            etL4.setText(prefs.getFloat("gripper", 60f).toString())
            etV.setText(prefs.getFloat("newLength", 60f).toString())
            etH1.setText(prefs.getFloat("high1", 60f).toString())
            etH2.setText(prefs.getFloat("high2", 60f).toString())
            etA1.setText(prefs.getFloat("phase", 60f).toString())

            btnSave.setOnClickListener {
                prefs.edit().apply {
                    putString("username", etUsername.text.toString())
                    putString("password", etPassword.text.toString())
                    putFloat("arm1", etL1.text.toString().toFloatOrNull() ?: 60f)
                    putFloat("arm2", etL2.text.toString().toFloatOrNull() ?: 60f)
                    putFloat("arm3", etL3.text.toString().toFloatOrNull() ?: 60f)
                    putFloat("gripper", etL4.text.toString().toFloatOrNull() ?: 60f)
                    putFloat("newLength", etV.text.toString().toFloatOrNull() ?: 60f)
                    putFloat("high1", etH1.text.toString().toFloatOrNull() ?: 60f)
                    putFloat("high2", etH2.text.toString().toFloatOrNull() ?: 60f)
                    putFloat("phase", etA1.text.toString().toFloatOrNull() ?: 60f)
                    apply()
                }
                Toast.makeText(this@MainActivity, "Settings saved", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun connectToDevice(address: String) {
        btJob?.cancel()
        appendLog("Connecting to $address…")
        updateConnectionStatus(false)

        btJob = scope.launch(Dispatchers.IO) {
            try {
                btDevice = btAdapter?.getRemoteDevice(address)
                btAdapter?.cancelDiscovery()
                btSocket = btDevice?.createInsecureRfcommSocketToServiceRecord(uuid)
                btSocket?.connect()

                withContext(Dispatchers.Main) {
                    @Suppress("DEPRECATION")
                    (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(250)
                    appendLog("Connected to $connectedDeviceName")
                    updateConnectionStatus(true)
                }

                val reader = BufferedReader(InputStreamReader(btSocket?.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    withContext(Dispatchers.Main) { appendLog(line) }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    appendLog("Connection error: ${e.message}")
                    updateConnectionStatus(false)
                }
            }
        }
    }

    private fun disconnectDevice() {
        btJob?.cancel()
        btSocket?.close()
        btSocket = null
        appendLog("Disconnected")
        updateConnectionStatus(false)
    }

    private fun sendDegrees() {
        scope.launch(Dispatchers.IO) {
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

    private fun refreshStatusBar() {
        binding.tvCurrentStatus.text = jointNames
            .mapIndexed { i, name -> "$name: ${degrees[i].toInt()}°" }
            .joinToString("  |  ")
    }

    private fun setLog(message: String) {
        logText = message
        binding.tvLog.text = logText
    }

    private fun appendLog(message: String) {
        logText = "› $message\n$logText"
        if (logText.length > 2000) logText = logText.take(2000)
        binding.tvLog.text = logText
    }

    private fun updateConnectionStatus(connected: Boolean) {
        binding.chipStatus.apply {
            text = if (connected) "● Connected: $connectedDeviceName" else "○ Disconnected"
            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (connected) R.color.status_connected else R.color.status_disconnected
                )
            )
        }
    }

    private fun initPrefs() {
        prefs = getSharedPreferences("RoboticArm", Context.MODE_PRIVATE)
        if (!prefs.contains("firstUser")) {
            prefs.edit().apply {
                putBoolean("firstUser", true)
                putString("username", "user")
                putString("password", "password")
                putFloat("arm1", 60f)
                putFloat("arm2", 60f)
                putFloat("arm3", 60f)
                putFloat("gripper", 60f)
                putFloat("high1", 60f)
                putFloat("high2", 60f)
                putFloat("phase", 60f)
                putFloat("newLength", 60f)
                apply()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        btSocket?.close()
    }
}
