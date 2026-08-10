package com.dctimerble.pro.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerEngineTest {
    @Test fun manualSolveStopsAtElapsedTime() {
        var clock = 0L
        val engine = TimerEngine(now = { clock })
        engine.dispatch(TimerEvent.Press)
        engine.dispatch(TimerEvent.Release)
        clock = 1_250L
        engine.dispatch(TimerEvent.Release)
        assertEquals(TimerState.Phase.STOPPED, engine.state.value.phase)
        assertEquals(1_250L, engine.state.value.lastSolveMs)
        assertEquals(1L, engine.state.value.solveSequence)
    }

    @Test fun solveListenerFiresExactlyOnceWithPenalty() {
        var clock = 0L
        var callbackCount = 0
        var callbackTime = 0L
        val engine = TimerEngine(now = { clock })
        engine.setSolveListener(object : TimerEngine.SolveListener {
            override fun onSolve(rawTimeMs: Long, penalty: TimerState.Penalty) {
                callbackCount++
                callbackTime = rawTimeMs
            }
        })
        engine.release()
        clock = 900L
        engine.release()
        engine.release()
        assertEquals(1, callbackCount)
        assertEquals(900L, callbackTime)
    }

    @Test fun repeatedExternalStopDoesNotDuplicateSolve() {
        var callbackCount = 0
        val engine = TimerEngine()
        engine.setSolveListener(object : TimerEngine.SolveListener {
            override fun onSolve(rawTimeMs: Long, penalty: TimerState.Penalty) {
                callbackCount++
            }
        })
        engine.externalRunning(100L)
        engine.externalStopped(800L)
        engine.externalStopped(900L)
        assertEquals(1, callbackCount)
        assertEquals(1L, engine.state.value.solveSequence)
    }

    @Test fun inspectionProducesPlusTwoAndDnf() {
        var clock = 0L
        val engine = TimerEngine(wcaInspection = true, now = { clock })
        engine.dispatch(TimerEvent.Press)
        engine.dispatch(TimerEvent.Release)
        clock = 15_100L
        engine.tick()
        assertEquals(TimerState.Penalty.PLUS_TWO, engine.state.value.penalty)
        clock = 17_100L
        engine.tick()
        assertEquals(TimerState.Penalty.DNF, engine.state.value.penalty)
    }
}