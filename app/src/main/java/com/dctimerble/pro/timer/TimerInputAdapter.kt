package com.dctimerble.pro.timer

/** Adapters used by BLE/Stackmat callbacks; no UI or Activity references. */
class TimerInputAdapter(private val engine: TimerEngine) {
    fun onExternalRunning(elapsedMs: Int) {
        engine.dispatch(TimerEvent.ExternalRunning(elapsedMs.toLong()))
    }

    fun onExternalStopped(timeMs: Int) {
        engine.dispatch(TimerEvent.ExternalStopped(timeMs.toLong()))
    }

    fun onDisconnected() {
        engine.dispatch(TimerEvent.ExternalDisconnected)
    }
}