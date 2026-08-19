package com.example.lanmultiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dctimerble.pro.timer.MainTimerScreen
import com.dctimerble.pro.timer.MainTimerViewModel
import com.dctimerble.pro.timer.TimerState

@Composable
fun LanMatchScreen(viewModel: LanViewModel) {
    val match by viewModel.match.collectAsStateWithLifecycle()
    val timer = remember { MainTimerViewModel() }
    val timerState by timer.state.collectAsStateWithLifecycle()
    var published by remember { mutableStateOf(false) }
    LaunchedEffect(timerState.phase, timerState.lastSolveMs) {
        if (timerState.phase == TimerState.Phase.STOPPED && timerState.lastSolveMs != null && !published) {
            published = true
            viewModel.publishFinish(timerState.lastSolveMs!!)
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F7F7))) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("房间：${match.roomName}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::leaveMatch) { Text("退出") }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("第 ${match.round} 轮 · ${match.playerName} vs ${match.opponentName}", fontWeight = FontWeight.Bold)
            Text("打乱：${match.scramble}", style = MaterialTheme.typography.bodySmall)
            match.opponentTimeMs?.let { Text("对手成绩：${it / 1000}.${(it % 1000).toString().padStart(3, '0')}", color = Color(0xFF448AFF)) }
            if (match.role == MatchRole.HOST) {
                Button(onClick = { published = false; viewModel.startRound() }, modifier = Modifier.fillMaxWidth()) { Text("开始本轮 PK") }
            } else Text("等待房主开始本轮…", color = Color.Gray)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { MainTimerScreen(timer, Modifier.fillMaxSize()) }
        match.message?.let { Text(it, Modifier.padding(12.dp), color = Color(0xFF448AFF)) }
    }
}