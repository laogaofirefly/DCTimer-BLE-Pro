package com.dctimerble.pro.timer

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Platform-independent main timer state machine.
 * Manual input, Stackmat and BLE adapters all feed this class instead of touching UI.
 */
class TimerEngine @JvmOverloads constructor(
    private val wcaInspection: Boolean = false,
    private val blindfold: Boolean = false,
    private val now: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var inspectionStarted = 0L
    private var solveStarted = 0L
    private var external = false
    private var solveSequence = 0L
    private var solveListener: SolveListener? = null

    interface SolveListener {
        fun onSolve(rawTimeMs: Long, penalty: TimerState.Penalty)
    }

    fun setSolveListener(listener: SolveListener?) {
        solveListener = listener
    }

    fun dispatch(event: TimerEvent) {
        when (event) {
            TimerEvent.Press -> pressInternal()
            TimerEvent.Release -> releaseInternal()
            TimerEvent.Cancel, TimerEvent.Reset, TimerEvent.ExternalDisconnected -> reset()
            is TimerEvent.ExternalRunning -> externalRunningInternal(event.elapsedMs)
            is TimerEvent.ExternalStopped -> externalStoppedInternal(event.elapsedMs)
        }
    }

    // Java-friendly bridge used while MainActivity is migrated.
    fun press() = dispatch(TimerEvent.Press)
    fun release() = dispatch(TimerEvent.Release)
    fun externalRunning(elapsedMs: Long) = dispatch(TimerEvent.ExternalRunning(elapsedMs))
    fun externalStopped(elapsedMs: Long) = dispatch(TimerEvent.ExternalStopped(elapsedMs))
    fun disconnect() = dispatch(TimerEvent.ExternalDisconnected)


    fun tick() {
        val current = _state.value
        when (current.phase) {
            TimerState.Phase.INSPECTING -> {
                val elapsed = (now() - inspectionStarted).coerceAtLeast(0L)
                _state.value = current.copy(
                    inspectionMs = elapsed,
                    penalty = when {
                        elapsed >= 17_000L -> TimerState.Penalty.DNF
                        elapsed >= 15_000L -> TimerState.Penalty.PLUS_TWO
                        else -> TimerState.Penalty.NONE
                    }
                )
            }
            TimerState.Phase.RUNNING -> {
                if (!external) _state.value = current.copy(elapsedMs = (now() - solveStarted).coerceAtLeast(0L))
            }
            else -> Unit
        }
    }

    private fun pressInternal() {
        // Press only arms the timer. A release commits the action, matching
        // physical timer behavior and preventing an accidental instant solve.
    }

    private fun releaseInternal() {
        when (_state.value.phase) {
            TimerState.Phase.READY -> {
                if (wcaInspection && !blindfold) startInspection() else startRunning()
            }
            TimerState.Phase.INSPECTING -> startRunning()
            TimerState.Phase.RUNNING -> stopRunning((now() - solveStarted).coerceAtLeast(100L))
            else -> Unit
        }
    }

    private fun startInspection() {
        inspectionStarted = now()
        _state.value = TimerState(phase = TimerState.Phase.INSPECTING)
    }

    private fun startRunning() {
        solveStarted = now()
        external = false
        _state.value = _state.value.copy(
            phase = TimerState.Phase.RUNNING,
            elapsedMs = 0L,
            canStart = true
        )
    }

    private fun stopRunning(elapsedMs: Long) {
        val finalElapsed = elapsedMs.coerceAtLeast(100L)
        solveSequence++
        _state.value = _state.value.copy(
            phase = TimerState.Phase.STOPPED,
            elapsedMs = finalElapsed,
            lastSolveMs = finalElapsed,
            solveSequence = solveSequence
        )
        solveListener?.onSolve(finalElapsed, _state.value.penalty)
    }

    private fun externalRunningInternal(elapsedMs: Long) {
        val current = _state.value
        // Ignore duplicate running packets while the external timer is already running.
        if (current.phase == TimerState.Phase.RUNNING && external) {
            _state.value = current.copy(elapsedMs = elapsedMs.coerceAtLeast(0L))
            return
        }
        external = true
        _state.value = TimerState(
            phase = TimerState.Phase.RUNNING,
            elapsedMs = elapsedMs.coerceAtLeast(0L),
            canStart = true
        )
    }

    private fun externalStoppedInternal(elapsedMs: Long) {
        // BLE/Stackmat protocols may repeat the final packet. Only the first
        // stop from RUNNING is a completed solve.
        if (_state.value.phase != TimerState.Phase.RUNNING) return
        external = true
        stopRunning(elapsedMs)
    }

    private fun reset() {
        external = false
        inspectionStarted = 0L
        solveStarted = 0L
        _state.value = TimerState()
    }
}