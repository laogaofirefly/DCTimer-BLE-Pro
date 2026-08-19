package com.example.lanmultiplayer

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dctimerble.pro.activity.MainActivity

private val MatchBackground = Color(0xFFF7F8FA)
private val MatchPrimary = Color(0xFF3F51B5)
private val MatchText = Color(0xFF202124)
private val MatchMuted = Color(0xFF6B7280)
private val MatchShape = RoundedCornerShape(12.dp)
private fun roundLabel(r: RoundResult) = when (r) { RoundResult.WIN -> "胜利"; RoundResult.LOSS -> "失败"; RoundResult.DRAW -> "平局"; RoundResult.PENDING -> "等待对手" }
private fun formatTime(ms: Long, dnf: Boolean) = if (dnf) "DNF" else "%.2f s".format(ms / 1000.0)

@Composable
fun LanMatchScreen(viewModel: LanViewModel) {
    val match by viewModel.match.collectAsStateWithLifecycle()
    BackHandler { viewModel.leaveMatch() }
    Column(Modifier.fillMaxSize().background(MatchBackground)) {
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("联机房间", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MatchText)
            Text(match.roomName, style = MaterialTheme.typography.bodyMedium, color = MatchMuted)
        }
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MatchPanel("房间状态") {
                Text("玩家（${match.players.size}）", fontWeight = FontWeight.Bold, color = MatchText)
                Text(match.players.joinToString("、"), color = MatchMuted)
                if (match.players.size < 2) Text("等待其他玩家加入…", color = Color(0xFFB26A00))
            }
            MatchPanel("比赛信息") {
                InfoRow("对手", match.opponentName)
                InfoRow("比分", "${match.myWins} : ${match.opponentWins}")
                InfoRow("本轮", roundLabel(match.roundResult))
                match.myTimeMs?.let { InfoRow("我方成绩", formatTime(it, match.myDnf)) }
                match.opponentTimeMs?.let { InfoRow("对手成绩", formatTime(it, match.opponentDnf)) }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                val intent = Intent(viewModel.getApplication(), MainActivity::class.java).apply { putExtra("lan_match_mode", true); putExtra("lan_room_name", match.roomName); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                viewModel.getApplication<android.app.Application>().startActivity(intent)
            }, modifier = Modifier.fillMaxWidth(), shape = MatchShape, colors = ButtonDefaults.buttonColors(containerColor = MatchPrimary)) { Text("进入原版计时器") }
            OutlinedButton(onClick = viewModel::leaveMatch, modifier = Modifier.fillMaxWidth(), shape = MatchShape) { Text("退出房间") }
        }
    }
}

@Composable private fun MatchPanel(title: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), shape = MatchShape, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MatchText); content() } } }
@Composable private fun InfoRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, color = MatchMuted, modifier = Modifier.weight(1f)); Text(value, fontWeight = FontWeight.Medium, color = MatchText) } }