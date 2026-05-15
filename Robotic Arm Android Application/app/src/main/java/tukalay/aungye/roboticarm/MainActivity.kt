package tukalay.aungye.roboticarm

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import tukalay.aungye.roboticarm.ui.theme.RoboticArmTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RoboticArmViewModel by viewModels()
    private var btAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btAdapter = checkBluetooth()
        setContent {
            RoboticArmTheme {
                RoboticArmApp(viewModel = viewModel, btAdapter = btAdapter)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun checkBluetooth(): BluetoothAdapter? {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return null
        }
        if (!adapter.isEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1)
        }
        return adapter
    }
}
