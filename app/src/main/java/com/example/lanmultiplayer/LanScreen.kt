package com.example.lanmultiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

private val LanBackground = Color(0xFFF7F8FA)
private val LanPrimary = Color(0xFF3F51B5)
private val LanText = Color(0xFF202124)
private val LanMuted = Color(0xFF6B7280)
private val LanShape = RoundedCornerShape(12.dp)

@Composable
fun LanScreen(viewModel: LanViewModel) {
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val roomName by viewModel.roomName.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(LanBackground)) {
        LanHeader("局域网联机", "创建或加入同一 Wi-Fi 下的房间")
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            item { StatusCard(state, stats) }
            item { LanPanel("玩家信息") { LanField(name, viewModel::setName, "玩家名称") } }
            item { LanPanel("创建房间") {
                LanField(roomName, viewModel::setRoomName, "房间名称")
                Button(onClick = viewModel::createRoom, modifier = Modifier.fillMaxWidth(), shape = LanShape, colors = ButtonDefaults.buttonColors(containerColor = LanPrimary)) { Text("创建局域网房间") }
            } }
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("可加入房间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LanText, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { if (searching) viewModel.stopSearch() else viewModel.search() }, shape = LanShape) { Text(if (searching) "停止搜索" else "搜索") }
            } }
            if (rooms.isEmpty()) item { EmptyRooms(searching) } else items(rooms, key = { "${it.name}-${it.host}-${it.tcpPort}" }) { RoomItem(it, viewModel::join) }
            message?.let { text -> item { Text(text, color = LanMuted, modifier = Modifier.padding(horizontal = 4.dp)) } }
        }
    }
}

@Composable private fun LanHeader(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 18.dp)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = LanText); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = LanMuted) } }
@Composable private fun LanPanel(title: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), shape = LanShape, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LanText); content() } } }
@Composable private fun LanField(value: String, onValueChange: (String) -> Unit, label: String) { OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = LanShape) }
@Composable private fun StatusCard(state: ConnectionState, stats: NetworkStats) { val color = when (state) { ConnectionState.CONNECTED -> Color(0xFF2E7D32); ConnectionState.CONNECTING -> Color(0xFFE68A00); ConnectionState.FAILED -> MaterialTheme.colorScheme.error; ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline }; Card(Modifier.fillMaxWidth(), shape = LanShape, colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50))); Spacer(Modifier.width(12.dp)); Column { Text("连接状态：${stateLabel(state)}", fontWeight = FontWeight.Bold, color = LanText); Text("RTT ${stats.rttMs.coerceAtLeast(0)} ms · ↑${stats.sent} ↓${stats.received}", style = MaterialTheme.typography.bodySmall, color = LanMuted) } } } }
@Composable private fun RoomItem(room: Room, onJoin: (Room) -> Unit) { Card(Modifier.fillMaxWidth(), shape = LanShape, colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(room.name, fontWeight = FontWeight.Bold, color = LanText); Text("${room.host}:${room.tcpPort}", style = MaterialTheme.typography.bodySmall, color = LanMuted); Text("${room.players}/${room.maxPlayers} 人 · ${room.mode}", style = MaterialTheme.typography.bodySmall, color = LanMuted) }; Button(onClick = { onJoin(room) }, shape = LanShape, colors = ButtonDefaults.buttonColors(containerColor = LanPrimary)) { Text("加入") } } } }
@Composable private fun EmptyRooms(searching: Boolean) { Card(Modifier.fillMaxWidth(), shape = LanShape, colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (searching) "正在搜索局域网房间…" else "暂无房间", color = LanText); Text("请确认设备连接到同一个 Wi-Fi", style = MaterialTheme.typography.bodySmall, color = LanMuted) } } }
private fun stateLabel(state: ConnectionState): String = when (state) { ConnectionState.CONNECTED -> "已连接"; ConnectionState.CONNECTING -> "连接中"; ConnectionState.FAILED -> "连接失败"; ConnectionState.DISCONNECTED -> "未连接" }