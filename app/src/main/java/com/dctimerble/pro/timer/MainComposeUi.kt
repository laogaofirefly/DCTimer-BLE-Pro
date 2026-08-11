package com.dctimerble.pro.timer

import android.app.Activity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
private val Ink = Color(0xFF202124)
private val Muted = Color(0xFF737780)
private val Line = Color(0xFFE3E5E9)
private val Soft = Color(0xFFF6F7F9)

object MainComposeUi {
    @JvmStatic fun install(activity: Activity) = (activity as com.dctimerble.pro.activity.MainActivity).setContent {
        MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = White, background = White, surface = White, onSurface = Ink)) { Root(activity) }
    }
}

@Composable private fun Root(activity: Activity, timer: MainTimerViewModel = MainTimerViewModel()) {
    var page by remember { mutableIntStateOf(0) }
    var group by remember { mutableIntStateOf(0) }
    var scramble by remember { mutableStateOf("R U R' U' F' U' F") }
    val records = remember { mutableStateListOf<SolveRecord>() }
    val bluetooth = remember { BluetoothCubeController(activity as com.dctimerble.pro.activity.MainActivity) }
    val timerState by timer.state.collectAsStateWithLifecycle()
    DisposableEffect(timer) { timer.setSolveListener { records.add(0, it.copy(scramble = scramble)) }; onDispose { timer.setSolveListener(null) } }
    Scaffold(containerColor = White, bottomBar = { BottomBar(page) { page = it } }) { pad ->
        when (page) {
            0 -> TimerPage(timer, timerState, group, { group = it }, scramble, { scramble = nextScramble() }, { page = 1 }, Modifier.padding(pad))
            1 -> OnlinePage(bluetooth, Modifier.padding(pad))
            2 -> ResultsPage(records, Modifier.padding(pad))
            else -> SettingsPage(Modifier.padding(pad))
        }
    }
}

@Composable private fun BottomBar(page: Int, select: (Int) -> Unit) { Row(Modifier.fillMaxWidth().background(White).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { BottomTab(Modifier.weight(1f), Icons.Outlined.Timer, "计时", page == 0) { select(0) }; BottomTab(Modifier.weight(1f), Icons.Outlined.Bluetooth, "联机", page == 1) { select(1) }; BottomTab(Modifier.weight(1f), Icons.Outlined.History, "成绩", page == 2) { select(2) }; BottomTab(Modifier.weight(1f), Icons.Outlined.Settings, "设置", page == 3) { select(3) } } }
@Composable private fun BottomTab(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, selected: Boolean, onClick: () -> Unit) { Surface(modifier.height(54.dp).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = if (selected) Soft else White, border = BorderStroke(1.dp, Line)) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, Modifier.size(20.dp)); Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } } }

@Composable private fun TimerPage(timer: MainTimerViewModel, state: TimerState, group: Int, setGroup: (Int) -> Unit, scramble: String, newScramble: () -> Unit, online: () -> Unit, modifier: Modifier) { Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Header("DCTimer", "专注每一次转动") { IconButton(onClick = online) { Icon(Icons.Outlined.Bluetooth, "联机") } }
    CardSection("计时分组") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("3×3", "2×2", "盲拧", "自定义").forEachIndexed { i, s -> FilterChip(selected = group == i, onClick = { setGroup(i) }, label = { Text(s) }, modifier = Modifier.weight(1f)) } } }
    CardSection("当前打乱") { Text(scramble, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(modifier = Modifier.weight(1f), onClick = {}) { Text("上一条") }; Button(modifier = Modifier.weight(1f), onClick = newScramble) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(4.dp)); Text("换一个") } } }
    Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line), elevation = CardDefaults.cardElevation(0.dp)) { Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onPress = { timer.press(); tryAwaitRelease(); timer.release() }) }, Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(timerText(state), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light); Text(phaseText(state.phase), color = Muted) } } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("按住计时区域开始", color = Muted, style = MaterialTheme.typography.bodySmall); OutlinedButton(onClick = { timer.cancel() }) { Text("重置") } }
} }

@Composable private fun OnlinePage(controller: BluetoothCubeController, modifier: Modifier) { val state by controller.uiState.collectAsStateWithLifecycle(); Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Header("联机", "连接智能魔方或计时器") { IconButton(onClick = { controller.scan(true) }) { Icon(Icons.Outlined.BluetoothSearching, "扫描") } }; CardSection("蓝牙设备") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(modifier = Modifier.weight(1f), onClick = { controller.scan(true) }) { Icon(Icons.Outlined.Bluetooth, null); Spacer(Modifier.width(5.dp)); Text(if (state.scanning) "扫描中" else "扫描设备") }; OutlinedButton(onClick = controller::stopScan) { Text("停止") } }; if (state.devices.isEmpty()) Text("暂无设备，点击扫描查找附近设备", color = Muted) else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { itemsIndexed(state.devices) { i, d -> DeviceRow(d.getName() ?: "未知设备", d.getAddress(), d.getConnected() != 0) { controller.connect(i) } } } }; state.connectedName?.let { CardSection("当前连接") { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text(it); Spacer(Modifier.weight(1f)); OutlinedButton(onClick = controller::disconnect) { Text("断开") } } } } } }

@Composable private fun ResultsPage(records: List<SolveRecord>, modifier: Modifier) { Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Header("成绩", "本次练习记录") { Text("${records.size} 次", color = Muted) }; if (records.isEmpty()) EmptyState("还没有成绩\n完成一次计时后会显示在这里") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { itemsIndexed(records) { i, r -> CardSection("#${i + 1}") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatMs(r.finalTimeMs), style = MaterialTheme.typography.titleLarge); Text(if (r.penalty == TimerState.Penalty.DNF) "DNF" else if (r.penalty == TimerState.Penalty.PLUS_TWO) "+2" else "正常", color = Muted) }; if (r.scramble.isNotEmpty()) Text(r.scramble, color = Muted, style = MaterialTheme.typography.bodySmall) } } } } }

@Composable private fun SettingsPage(modifier: Modifier) { Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Header("设置", "按你的习惯自由调整") { Icon(Icons.Outlined.Tune, "设置") }; CardSection("计时") { Setting("WCA 检查", "关闭", Icons.Outlined.Visibility); Setting("计时精度", "0.001 秒", Icons.Outlined.Timer); Setting("声音与震动", "跟随系统", Icons.Outlined.VolumeUp) }; CardSection("外观") { Setting("主题颜色", "可自定义", Icons.Outlined.Palette); Setting("界面主题", "纯白", Icons.Outlined.LightMode) }; CardSection("关于") { Setting("版本", "DCTimer-BLE Pro", Icons.Outlined.Info) } } }
@Composable private fun Header(title: String, subtitle: String, trailing: @Composable RowScope.() -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }; Row(content = trailing) } }
@Composable private fun CardSection(title: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(White), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line), elevation = CardDefaults.cardElevation(0.dp)) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, color = Muted, style = MaterialTheme.typography.labelMedium); content() } } }
@Composable private fun Setting(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(20.dp), tint = Muted); Spacer(Modifier.width(12.dp)); Text(title, Modifier.weight(1f)); Text(value, color = Muted); Icon(Icons.Outlined.ChevronRight, null, tint = Muted) } }
@Composable private fun DeviceRow(name: String, address: String, connected: Boolean, click: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = click).background(Soft, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Bluetooth, null, tint = Ink); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Medium); Text(address, color = Muted, style = MaterialTheme.typography.bodySmall) }; Text(if (connected) "已连接" else "连接", color = Muted); Icon(Icons.Outlined.ChevronRight, null, tint = Muted) } }
@Composable private fun EmptyState(text: String) { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text(text, color = Muted, textAlign = TextAlign.Center) } }
private fun nextScramble() = listOf("R U R' U' F' U' F", "F R U R' U' F'", "L U' L' U F U' F'").random()
private fun formatMs(ms: Long) = "%d.%03d".format(ms / 1000, ms % 1000)
private fun phaseText(p: TimerState.Phase) = when (p) { TimerState.Phase.READY -> "准备就绪"; TimerState.Phase.INSPECTING -> "观察中"; TimerState.Phase.RUNNING -> "计时中"; TimerState.Phase.STOPPED -> "已停止" }
private fun timerText(s: TimerState) = if (s.phase == TimerState.Phase.READY) "0.000" else formatMs(s.elapsedMs)