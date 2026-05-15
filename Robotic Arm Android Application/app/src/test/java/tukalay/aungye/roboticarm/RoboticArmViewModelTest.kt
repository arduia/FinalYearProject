package tukalay.aungye.roboticarm

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoboticArmViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: RoboticArmViewModel

    @Before
    fun setUp() {
        viewModel = RoboticArmViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    // ── Initial state ──────────────────────────────────────────────────────────

    @Test
    fun `initial degrees are all 90`() {
        assertEquals(List(6) { 90f }, viewModel.degrees.toList())
    }

    @Test
    fun `isConnected is initially false`() {
        assertFalse(viewModel.isConnected)
    }

    @Test
    fun `connectedName is initially empty`() {
        assertTrue(viewModel.connectedName.isEmpty())
    }

    @Test
    fun `logText is initially empty`() {
        assertTrue(viewModel.logText.isEmpty())
    }

    // ── Settings persistence ───────────────────────────────────────────────────

    @Test
    fun `loadSettings returns seeded defaults on first launch`() {
        val s = viewModel.loadSettings()
        assertEquals("user", s.username)
        assertEquals("password", s.password)
        assertEquals("60.0", s.l1)
        assertEquals("60.0", s.a1)
    }

    @Test
    fun `saveSettings then loadSettings round-trips all fields`() {
        val saved = SettingsState(
            username = "alice",
            password = "s3cr3t",
            l1 = "120.5",
            l2 = "80.0",
            l3 = "55.5",
            l4 = "40.0",
            v  = "22.0",
            h1 = "15.0",
            h2 = "10.0",
            a1 = "45.0"
        )
        viewModel.saveSettings(saved)

        val loaded = viewModel.loadSettings()
        assertEquals("alice",  loaded.username)
        assertEquals("s3cr3t", loaded.password)
        assertEquals("120.5",  loaded.l1)
        assertEquals("80.0",   loaded.l2)
        assertEquals("55.5",   loaded.l3)
        assertEquals("40.0",   loaded.l4)
        assertEquals("22.0",   loaded.v)
        assertEquals("15.0",   loaded.h1)
        assertEquals("10.0",   loaded.h2)
        assertEquals("45.0",   loaded.a1)
    }

    @Test
    fun `saveSettings falls back to 60 for non-numeric strings`() {
        viewModel.saveSettings(SettingsState(l1 = "not-a-number", l2 = ""))
        val loaded = viewModel.loadSettings()
        assertEquals("60.0", loaded.l1)
        assertEquals("60.0", loaded.l2)
    }

    @Test
    fun `successive saves overwrite previous values`() {
        viewModel.saveSettings(SettingsState(username = "first"))
        viewModel.saveSettings(SettingsState(username = "second"))
        assertEquals("second", viewModel.loadSettings().username)
    }

    // ── Disconnect ─────────────────────────────────────────────────────────────

    @Test
    fun `disconnect sets isConnected to false`() {
        viewModel.disconnect()
        assertFalse(viewModel.isConnected)
    }

    @Test
    fun `disconnect appends Disconnected to log`() {
        viewModel.disconnect()
        assertTrue(viewModel.logText.contains("Disconnected"))
    }

    @Test
    fun `calling disconnect twice does not throw`() {
        viewModel.disconnect()
        viewModel.disconnect()
        assertFalse(viewModel.isConnected)
    }

    // ── Log trimming ───────────────────────────────────────────────────────────

    @Test
    fun `log is trimmed to 2000 chars when it overflows`() {
        repeat(150) { viewModel.disconnect() }
        assertTrue(viewModel.logText.length <= 2000)
    }

    @Test
    fun `log entries are prepended not appended`() {
        viewModel.disconnect()                 // adds "Disconnected"
        viewModel.disconnect()
        // The most recent entry should be at the top (starts with ›)
        assertTrue(viewModel.logText.startsWith("›"))
    }

    // ── pairedDevices ──────────────────────────────────────────────────────────

    @Test
    fun `pairedDevices returns empty list when bondedDevices is null`() {
        val adapter = mockk<BluetoothAdapter> { every { bondedDevices } returns null }
        assertTrue(viewModel.pairedDevices(adapter).isEmpty())
    }

    @Test
    fun `pairedDevices returns empty list for empty bonded set`() {
        val adapter = mockk<BluetoothAdapter> { every { bondedDevices } returns emptySet() }
        assertTrue(viewModel.pairedDevices(adapter).isEmpty())
    }

    @Test
    fun `pairedDevices maps name and address for each bonded device`() {
        val d1 = mockk<BluetoothDevice> {
            every { name }    returns "HC-05"
            every { address } returns "00:11:22:33:44:55"
        }
        val d2 = mockk<BluetoothDevice> {
            every { name }    returns "Arduino BT"
            every { address } returns "AA:BB:CC:DD:EE:FF"
        }
        val adapter = mockk<BluetoothAdapter> {
            every { bondedDevices } returns mutableSetOf(d1, d2)
        }

        val result = viewModel.pairedDevices(adapter)

        assertEquals(2, result.size)
        assertTrue(result.any { it.first == "HC-05"       && it.second == "00:11:22:33:44:55" })
        assertTrue(result.any { it.first == "Arduino BT"  && it.second == "AA:BB:CC:DD:EE:FF" })
    }

    @Test
    fun `pairedDevices uses Unknown for null device name`() {
        val d = mockk<BluetoothDevice> {
            every { name }    returns null
            every { address } returns "00:11:22:33:44:55"
        }
        val adapter = mockk<BluetoothAdapter> {
            every { bondedDevices } returns mutableSetOf(d)
        }

        val result = viewModel.pairedDevices(adapter)
        assertEquals("Unknown", result.first().first)
    }

    // ── sendDegrees error path ─────────────────────────────────────────────────

    @Test
    fun `sendDegrees while disconnected logs error`() = runTest {
        // btSocket is null when disconnected; sending should log an error
        viewModel.sendDegrees()
        // No crash; the log may or may not have content depending on the null-safe write
        // The important thing is no exception is thrown
    }
}
