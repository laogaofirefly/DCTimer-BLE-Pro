package com.dctimerble.pro.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Compose-facing owner for the new main timer engine. */
class MainTimerViewModel(
    wcaInspection: Boolean = false,
    blindfold: Boolean = false
) : ViewModel() {
    private val engine = TimerEngine(wcaInspection, blindfold)
    val state: StateFlow<TimerState> = engine.state
    private var ticker: Job? = null
    private var lastDeliveredSolve = 0L
    private var solveListener: ((SolveRecord) -> Unit)? = null

    fun press() = dispatch(TimerEvent.Press)
    fun release() = dispatch(TimerEvent.Release)
    fun cancel() = dispatch(TimerEvent.Cancel)
    fun externalRunning(ms: Long) = dispatch(TimerEvent.ExternalRunning(ms))
    fun externalStopped(ms: Long) = dispatch(TimerEvent.ExternalStopped(ms))
    fun tick() {
        engine.tick()
        deliverSolveIfNeeded()
    }

    fun setSolveListener(listener: ((SolveRecord) -> Unit)?) {
        solveListener = listener
    }

    private fun dispatch(event: TimerEvent) {
        engine.dispatch(event)
        deliverSolveIfNeeded()
        ensureTicker()
    }

    private fun deliverSolveIfNeeded() {
        val current = engine.state.value
        if (current.phase == TimerState.Phase.STOPPED && current.solveSequence > lastDeliveredSolve) {
            current.toSolveRecord()?.let { solveListener?.invoke(it) }
            lastDeliveredSolve = current.solveSequence
        }
    }

    private fun ensureTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (isActive) {
                engine.tick()
                deliverSolveIfNeeded()
                if (engine.state.value.phase == TimerState.Phase.READY ||
                    engine.state.value.phase == TimerState.Phase.STOPPED) break
                delay(16L)
            }
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}