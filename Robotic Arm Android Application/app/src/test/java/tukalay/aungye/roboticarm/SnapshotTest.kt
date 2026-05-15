package tukalay.aungye.roboticarm

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tukalay.aungye.roboticarm.ui.theme.RoboticArmTheme

@RunWith(JUnit4::class)
class SnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    // ── ConnectionChip ─────────────────────────────────────────────────────────

    @Test
    fun connectionChip_disconnected() {
        paparazzi.snapshot {
            RoboticArmTheme {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ConnectionChip(isConnected = false, connectedName = "")
                }
            }
        }
    }

    @Test
    fun connectionChip_connected() {
        paparazzi.snapshot {
            RoboticArmTheme {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ConnectionChip(isConnected = true, connectedName = "HC-05")
                }
            }
        }
    }

    // ── JointCard ──────────────────────────────────────────────────────────────

    @Test
    fun jointCard_midPosition() {
        paparazzi.snapshot {
            RoboticArmTheme {
                JointCard(name = "Shoulder", value = 90f, onValueChange = {})
            }
        }
    }

    @Test
    fun jointCard_minPosition() {
        paparazzi.snapshot {
            RoboticArmTheme {
                JointCard(name = "Gripper", value = 0f, onValueChange = {})
            }
        }
    }

    @Test
    fun jointCard_maxPosition() {
        paparazzi.snapshot {
            RoboticArmTheme {
                JointCard(name = "Elbow", value = 180f, onValueChange = {})
            }
        }
    }

    // ── LogCard ────────────────────────────────────────────────────────────────

    @Test
    fun logCard_empty() {
        paparazzi.snapshot {
            RoboticArmTheme {
                LogCard(logText = "")
            }
        }
    }

    @Test
    fun logCard_withMultipleEntries() {
        paparazzi.snapshot {
            RoboticArmTheme {
                LogCard(
                    logText = "› Connected to HC-05\n› Connecting to 00:11:22:33:44:55…\n› Reading…"
                )
            }
        }
    }

    // ── DeviceDialog ───────────────────────────────────────────────────────────

    @Test
    fun deviceDialog_empty() {
        paparazzi.snapshot {
            RoboticArmTheme {
                DeviceDialog(
                    devices = emptyList(),
                    onDismiss = {},
                    onSelect = { _, _ -> }
                )
            }
        }
    }

    @Test
    fun deviceDialog_withDevices() {
        paparazzi.snapshot {
            RoboticArmTheme {
                DeviceDialog(
                    devices = listOf(
                        Pair("HC-05",      "00:11:22:33:44:55"),
                        Pair("Arduino BT", "AA:BB:CC:DD:EE:FF"),
                        Pair("RobotArm",   "11:22:33:44:55:66")
                    ),
                    onDismiss = {},
                    onSelect = { _, _ -> }
                )
            }
        }
    }

    // ── SettingsDialog ─────────────────────────────────────────────────────────

    @Test
    fun settingsDialog_defaultValues() {
        paparazzi.snapshot {
            RoboticArmTheme {
                SettingsDialog(
                    initial = SettingsState(
                        username = "user",
                        password = "password",
                        l1 = "60.0", l2 = "60.0", l3 = "60.0", l4 = "60.0",
                        v  = "60.0", h1 = "60.0", h2 = "60.0", a1 = "60.0"
                    ),
                    onDismiss = {},
                    onSave = {}
                )
            }
        }
    }

    @Test
    fun settingsDialog_customValues() {
        paparazzi.snapshot {
            RoboticArmTheme {
                SettingsDialog(
                    initial = SettingsState(
                        username = "alice",
                        password = "s3cr3t",
                        l1 = "120.5", l2 = "80.0", l3 = "55.5", l4 = "40.0",
                        v  = "22.0",  h1 = "15.0", h2 = "10.0", a1 = "45.0"
                    ),
                    onDismiss = {},
                    onSave = {}
                )
            }
        }
    }
}
