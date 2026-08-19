package com.example.lanmultiplayer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dctimerble.pro.activity.MainActivity

private fun roundLabel(r: RoundResult) = when (r) { RoundResult.WIN -> "胜利"; RoundResult.LOSS -> "失败"; RoundResult.DRAW -> "平局"; RoundResult.PENDING -> "等待对手" }
private fun formatTime(ms: Long, dnf: Boolean) = if (dnf) "DNF" else "%.2f s".format(ms / 1000.0)

/**
 * 房间等待页。计时、分组选择、打乱选择全部交给原版 MainActivity，
 * 这里不复制计时器 UI，避免出现两套计时器。
 */
@Composable
fun LanMatchScreen(viewModel: LanViewModel) {
    val match by viewModel.match.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF7F7F7)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("房间：${match.roomName}", fontWeight = FontWeight.Bold)
        Text("已进入联机房间。请选择分组和打乱后，使用原版计时器完成本轮。")
        Text("房间玩家（${match.players.size}）：${match.players.joinToString("、")}", color = Color.Gray)
        Text("对手：${match.opponentName}", color = Color.Gray)
        Text("比分：${match.myWins} : ${match.opponentWins}", fontWeight = FontWeight.Bold)
        Text("本轮：${roundLabel(match.roundResult)}")
        match.myTimeMs?.let { Text("我方：${formatTime(it, match.myDnf)}") }
        match.opponentTimeMs?.let { Text("对手：${formatTime(it, match.opponentDnf)}") }
        Button(
            onClick = {
                val intent = Intent(viewModel.getApplication(), MainActivity::class.java)
                intent.putExtra("lan_match_mode", true)
                intent.putExtra("lan_room_name", match.roomName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                viewModel.getApplication<android.app.Application>().startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("进入原版计时器") }
        OutlinedButton(onClick = viewModel::leaveMatch, modifier = Modifier.fillMaxWidth()) { Text("退出房间") }
    }
}