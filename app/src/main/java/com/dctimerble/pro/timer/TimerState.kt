package com.dctimerble.pro.timer

/** Immutable state exposed by the rewritten main timer engine. */
data class TimerState(
    val phase: Phase = Phase.READY,
    val elapsedMs: Long = 0L,
    val inspectionMs: Long = 0L,
    val penalty: Penalty = Penalty.NONE,
    val canStart: Boolean = true,
    val lastSolveMs: Long? = null,
    val solveSequence: Long = 0L
) {
    enum class Phase { READY, INSPECTING, RUNNING, STOPPED }
    enum class Penalty { NONE, PLUS_TWO, DNF }
}

sealed interface TimerEvent {
    data object Press : TimerEvent
    data object Release : TimerEvent
    data object Cancel : TimerEvent
    data object Reset : TimerEvent
    data class ExternalRunning(val elapsedMs: Long) : TimerEvent
    data class ExternalStopped(val elapsedMs: Long) : TimerEvent
    data object ExternalDisconnected : TimerEvent
}
