package com.dctimerble.pro.timer

import androidx.lifecycle.ViewModel
import com.dctimerble.pro.activity.MainActivity
import com.dctimerble.pro.model.BLEDevice
import com.dctimerble.pro.model.SmartCube
import com.dctimerble.pro.util.BluetoothTools
import com.dctimerble.pro.util.SmartTimerProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Compose-facing facade for Bluetooth cubes and Bluetooth timers.
 * Protocol discovery, scanning, GATT and device-specific parsing stay in BluetoothTools.
 */
class BluetoothCubeController(private val activity: MainActivity) : ViewModel() {
    data class UiState(
        val devices: List<BLEDevice> = emptyList(),
        val scanning: Boolean = false,
        val connectedName: String? = null,
        val cubeState: String? = null,
        val timerPhase: TimerPhase = TimerPhase.IDLE,
        val timerMs: Int = 0,
        val error: String? = null
    )

    enum class TimerPhase { IDLE, READY, RUNNING, STOPPED, DISCONNECTED }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val bluetooth = BluetoothTools(activity)

    init {
        bluetooth.setDeviceListener(object : BluetoothTools.DeviceListener {
            override fun onDevicesChanged(devices: List<BLEDevice>) = update { copy(devices = devices) }
            override fun onScanStateChanged(scanning: Boolean) = update { copy(scanning = scanning) }
            override fun onConnected(device: BLEDevice) = update { copy(connectedName = device.name, error = null) }
            override fun onDisconnected(device: BLEDevice) = update { copy(connectedName = null, timerPhase = TimerPhase.DISCONNECTED) }
        })
        bluetooth.setCubeStateChangedCallback(object : SmartCube.StateChangedCallback {
            override fun onScrambled(cube: SmartCube) = update { copy(cubeState = cube.cubeState) }
            override fun onSolved(cube: SmartCube) = update { copy(cubeState = cube.cubeState) }
        })
        bluetooth.setTimerStateCallback(object : SmartTimerProtocol.StateCallback {
            override fun onTimerIdle(time: Int) = timer(TimerPhase.IDLE, time)
            override fun onTimerReady(time: Int) = timer(TimerPhase.READY, time)
            override fun onTimerRunning(time: Int) = timer(TimerPhase.RUNNING, time)
            override fun onTimerStopped(time: Int) = timer(TimerPhase.STOPPED, time)
            override fun onTimerDisconnected() = update { copy(timerPhase = TimerPhase.DISCONNECTED, connectedName = null) }
        })
        bluetooth.initBluetoothAdapter()
    }

    fun scan(allTimingDevices: Boolean = true) {
        bluetooth.setScanAllTimingDevices(allTimingDevices)
        bluetooth.startScan()
    }

    fun stopScan() = bluetooth.stopScan()
    fun connect(index: Int) = bluetooth.connectDevice(index)
    fun disconnect() = bluetooth.disconnect()
    fun isBluetoothEnabled() = bluetooth.isBluetoothEnabled()
    fun devices(): List<BLEDevice> = bluetooth.devices

    private fun timer(phase: TimerPhase, time: Int) = update { copy(timerPhase = phase, timerMs = time) }
    private fun update(block: UiState.() -> UiState) { _uiState.value = block(_uiState.value) }

    override fun onCleared() {
        bluetooth.stopScan()
        bluetooth.disconnect()
        super.onCleared()
    }
}
