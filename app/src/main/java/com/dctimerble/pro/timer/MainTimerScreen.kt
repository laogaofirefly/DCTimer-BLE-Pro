package com.dctimerble.pro.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainTimerScreen(
    viewModel: MainTimerViewModel,
    modifier: Modifier = Modifier,
    onSolve: (SolveRecord) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.solveSequence) {
        state.toSolveRecord()?.let(onSolve)
    }
    // ViewModel owns the ticker.

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        viewModel.press()
                        tryAwaitRelease()
                        viewModel.release()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(timerLabel(state), style = MaterialTheme.typography.displayLarge)
            Text(state.phase.name, style = MaterialTheme.typography.labelLarge)
            if (state.penalty != TimerState.Penalty.NONE) {
                Text(state.penalty.name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun timerLabel(state: TimerState): String {
    return when (state.phase) {
        TimerState.Phase.READY -> "0.00"
        TimerState.Phase.INSPECTING -> (state.inspectionMs / 1000L).toString()
        TimerState.Phase.RUNNING, TimerState.Phase.STOPPED -> {
            val ms = state.elapsedMs
            "%d.%03d".format(ms / 1000L, ms % 1000L)
        }
    }
}