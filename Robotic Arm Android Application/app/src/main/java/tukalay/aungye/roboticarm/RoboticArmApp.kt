@file:OptIn(ExperimentalMaterial3Api::class)

package tukalay.aungye.roboticarm

import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ConnectedColor = Color(0xFF2E7D32)
private val DisconnectedColor = Color(0xFF78909C)
private val LogBg = Color(0xFF1E272C)
private val LogText = Color(0xFF80CBC4)
private val LogHeader = Color(0xFF546E7A)

private val jointNames = listOf("Shoulder", "Base", "Elbow", "Wrist", "Wrist Spin", "Gripper")

@Composable
fun RoboticArmApp(viewModel: RoboticArmViewModel, btAdapter: BluetoothAdapter?) {
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Robotic Arm Controller") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Connect") },
                                onClick = { showMenu = false; showDeviceDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Disconnect") },
                                onClick = { showMenu = false; viewModel.disconnect() }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = { showMenu = false; showSettingsDialog = true }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            ConnectionChip(
                isConnected = viewModel.isConnected,
                connectedName = viewModel.connectedName
            )

            Spacer(Modifier.height(8.dp))

            jointNames.forEachIndexed { index, name ->
                JointCard(
                    name = name,
                    value = viewModel.degrees[index],
                    onValueChange = {
                        viewModel.degrees[index] = it
                        if (viewModel.isConnected) viewModel.sendDegrees()
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            LogCard(logText = viewModel.logText)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.sendDegrees() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SEND COMMAND",
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeviceDialog) {
        DeviceDialog(
            devices = btAdapter?.let { viewModel.pairedDevices(it) } ?: emptyList(),
            onDismiss = { showDeviceDialog = false },
            onSelect = { address, name ->
                showDeviceDialog = false
                btAdapter?.let { viewModel.connectToDevice(it, address, name) }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            initial = viewModel.loadSettings(),
            onDismiss = { showSettingsDialog = false },
            onSave = {
                viewModel.saveSettings(it)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
private fun ConnectionChip(isConnected: Boolean, connectedName: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isConnected) ConnectedColor else DisconnectedColor
    ) {
        Text(
            text = if (isConnected) "● Connected: $connectedName" else "○ Disconnected",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun JointCard(name: String, value: Float, onValueChange: (Float) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${value.toInt()}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..180f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LogCard(logText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LogBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LOG OUTPUT",
                style = MaterialTheme.typography.labelSmall,
                color = LogHeader,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = logText.ifEmpty { "Waiting for connection…" },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                ),
                color = LogText,
                modifier = Modifier.defaultMinSize(minHeight = 64.dp)
            )
        }
    }
}

@Composable
private fun DeviceDialog(
    devices: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Bluetooth Device") },
        text = {
            if (devices.isEmpty()) {
                Text("No paired devices found.\nPair a device in Bluetooth settings first.")
            } else {
                Box(modifier = Modifier.heightIn(max = 280.dp)) {
                    LazyColumn {
                        items(devices) { (name, address) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(address, name) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    address,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsDialog(
    initial: SettingsState,
    onDismiss: () -> Unit,
    onSave: (SettingsState) -> Unit
) {
    var s by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuration") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SettingsSection("CREDENTIALS")
                SettingsField("Username", s.username, Modifier.fillMaxWidth()) {
                    s = s.copy(username = it)
                }
                SettingsField("Password", s.password, Modifier.fillMaxWidth(), isPassword = true) {
                    s = s.copy(password = it)
                }

                SettingsSection("ARM GEOMETRY (mm)")
                Row(Modifier.fillMaxWidth()) {
                    SettingsField("L1", s.l1, Modifier.weight(1f).padding(end = 4.dp)) {
                        s = s.copy(l1 = it)
                    }
                    SettingsField("L2", s.l2, Modifier.weight(1f).padding(start = 4.dp)) {
                        s = s.copy(l2 = it)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    SettingsField("L3", s.l3, Modifier.weight(1f).padding(end = 4.dp)) {
                        s = s.copy(l3 = it)
                    }
                    SettingsField("L4", s.l4, Modifier.weight(1f).padding(start = 4.dp)) {
                        s = s.copy(l4 = it)
                    }
                }

                SettingsSection("ADDITIONAL PARAMETERS")
                Row(Modifier.fillMaxWidth()) {
                    SettingsField("V (Gripper)", s.v, Modifier.weight(1f).padding(end = 4.dp)) {
                        s = s.copy(v = it)
                    }
                    SettingsField("A1 (Phase)", s.a1, Modifier.weight(1f).padding(start = 4.dp)) {
                        s = s.copy(a1 = it)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    SettingsField("H1", s.h1, Modifier.weight(1f).padding(end = 4.dp)) {
                        s = s.copy(h1 = it)
                    }
                    SettingsField("H2", s.h2, Modifier.weight(1f).padding(start = 4.dp)) {
                        s = s.copy(h2 = it)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(s) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Decimal
        ),
        modifier = modifier.padding(bottom = 8.dp)
    )
}
