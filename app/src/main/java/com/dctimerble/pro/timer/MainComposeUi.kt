package com.dctimerble.pro.timer

import android.app.Activity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val White = Color.White
private val TextMain = Color(0xFF202124)
private val TextSecondary = Color(0xFF747880)
private val Border = Color(0xFFE4E6EA)
private val Soft = Color(0xFFF7F8FA)

object MainComposeUi {
    @JvmStatic fun install(activity: Activity) = activity.setContent {
        MaterialTheme(colorScheme = lightColorScheme(primary = TextMain, onPrimary = White, background = White, surface = White, onSurface = TextMain)) { MainComposeRoot(activity) }
    }
}

@Composable
private fun MainComposeRoot(activity: Activity, viewModel: MainTimerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var page by remember { mutableIntStateOf(0) }
    var mode by remember { mutableIntStateOf(0) }
    val bluetooth = remember { mutableStateOf<BluetoothCubeController?>(BluetoothCubeController(activity as com.dctimerble.pro.activity.MainActivity)) }
    Scaffold(containerColor = White, bottomBar = {
        Row(Modifier.fillMaxWidth().background(White).border(BorderStroke(1.dp, Border)).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BottomTab(Icons.Outlined.Timer, "计时", page == 0) { page = 0 }
            BottomTab(Icons.Outlined.Bluetooth, "联机", page == 1) { page = 1 }
        }
    }) { inset ->
        when (page) {
            0 -> TimerPage(viewModel, mode, { mode = it }, { page = 1 }, Modifier.padding(inset))
            else -> OnlinePage(bluetooth, Modifier.padding(inset))
        }
    }
}

@Composable
private fun TimerPage(viewModel: MainTimerViewModel, mode: Int, onMode: (Int) -> Unit, openOnline: () -> Unit, modifier: Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("DCTimer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("专注每一次转动", color = TextSecondary, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderIcon(Icons.Outlined.History, "成绩")
                HeaderIcon(Icons.Outlined.Settings, "设置")
                HeaderIcon(Icons.Outlined.Bluetooth, "联机", openOnline)
            }
        }
        SectionCard("计时分组") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("3×3", "2×2", "盲拧", "自定义").forEachIndexed { i, label ->
                    FilterChip(selected = mode == i, onClick = { onMode(i) }, label = { Text(label) }, modifier = Modifier.weight(1f))
                }
            }
        }
        SectionCard("当前打乱") {
            Text("R U R' U' F' U' F", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(Modifier.weight(1f), onClick = {}) { Text("上一条") }
                Button(Modifier.weight(1f), onClick = {}) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(5.dp)); Text("换一个") }
            }
        }
        Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Border), elevation = CardDefaults.cardElevation(0.dp)) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onPress = { viewModel.press(); tryAwaitRelease(); viewModel.release() }) }, Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(timerLabel(state), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light); Text(phaseLabel(state.phase), color = TextSecondary) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("按住计时，松开停止", color = TextSecondary, style = MaterialTheme.typography.bodySmall); OutlinedButton(onClick = { viewModel.cancel() }) { Text("重置") } }
    }
}

@Composable private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(White), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Border), elevation = CardDefaults.cardElevation(0.dp)) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = { Text(title, color = TextSecondary, style = MaterialTheme.typography.labelMedium); content() }) } }
@Composable private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (() -> Unit)? = null) { Surface(Modifier.size(48.dp).clickable { onClick?.invoke() }, color = White, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Border)) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, label, Modifier.size(19.dp)); Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary) } } }
@Composable private fun BottomTab(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) { Surface(Modifier.weight(1f).height(52.dp).clickable(onClick = onClick), color = if (selected) Soft else White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Border)) { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(21.dp)); Spacer(Modifier.width(8.dp)); Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } } }

@Composable
private fun OnlinePage(controller: MutableState<BluetoothCubeController?>, modifier: Modifier) {
    val c = controller.value
    val state = c?.uiState?.collectAsStateWithLifecycle()?.value
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("联机", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SectionCard("蓝牙设备") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(Modifier.weight(1f), onClick = { c?.scan(true) }) { Icon(Icons.Outlined.Bluetooth, null); Spacer(Modifier.width(5.dp)); Text(if (state?.scanning == true) "扫描中" else "扫描设备") }; OutlinedButton(onClick = { c?.stopScan() }) { Text("停止") } }
            if (state == null || state.devices.isEmpty()) Text("点击“扫描设备”查找附近的魔方或智能计时器", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { itemsIndexed(state.devices) { index, device -> DeviceRow(device.getName() ?: "未知设备", device.getAddress(), device.getConnected() != 0) { c?.connect(index) } } }
        }
        state?.connectedName?.let { name -> SectionCard("当前连接") { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text(name); Spacer(Modifier.weight(1f)); OutlinedButton(onClick = { c?.disconnect() }) { Text("断开") } } } }
    }
}
@Composable private fun DeviceRow(name: String, address: String, connected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(Soft, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Bluetooth, null); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Medium); Text(address, color = TextSecondary, style = MaterialTheme.typography.bodySmall) }; Text(if (connected) "已连接" else "连接", color = TextSecondary); Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary) } }
private fun phaseLabel(p: TimerState.Phase) = when (p) { TimerState.Phase.READY -> "准备就绪"; TimerState.Phase.INSPECTING -> "观察中"; TimerState.Phase.RUNNING -> "计时中"; TimerState.Phase.STOPPED -> "已停止" }
private fun timerLabel(s: TimerState) = if (s.phase == TimerState.Phase.READY) "0.000" else "%d.%03d".format(s.elapsedMs / 1000, s.elapsedMs % 1000)