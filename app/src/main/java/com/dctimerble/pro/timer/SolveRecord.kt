package com.dctimerble.pro.timer

/** A completed solve produced by every timer input source. */
data class SolveRecord(
    val rawTimeMs: Long,
    val penalty: TimerState.Penalty,
    val scramble: String = ""
) {
    val finalTimeMs: Long
        get() = when (penalty) {
            TimerState.Penalty.NONE -> rawTimeMs
            TimerState.Penalty.PLUS_TWO -> rawTimeMs + 2_000L
            TimerState.Penalty.DNF -> rawTimeMs
        }
}

fun TimerState.toSolveRecord(scramble: String = ""): SolveRecord? {
    val time = lastSolveMs ?: return null
    if (phase != TimerState.Phase.STOPPED || solveSequence <= 0L) return null
    return SolveRecord(time, penalty, scramble)
}