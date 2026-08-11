package com.dctimerble.pro.timer

import android.app.Activity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val AppWhite = Color.White
private val Ink = Color(0xFF202124)
private val Line = Color(0xFFE5E7EB)
private val Muted = Color(0xFF737781)

object MainComposeUi {
    @JvmStatic fun install(activity: Activity) {
        activity.setContent { MaterialTheme(colorScheme = lightColorScheme(background = AppWhite, surface = AppWhite, primary = Ink, onSurface = Ink)) { MainComposeRoot() } }
    }
}

@Composable
private fun MainComposeRoot(viewModel: MainTimerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var page by remember { mutableIntStateOf(0) }
    var mode by remember { mutableIntStateOf(0) }
    val solves = remember { mutableStateListOf<SolveRecord>() }
    val bluetooth = remember { mutableStateOf<BluetoothCubeController?>(null) }
    Scaffold(containerColor = AppWhite, bottomBar = {
        Row(Modifier.fillMaxWidth().background(AppWhite).border(1.dp, Line).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BottomAction("◷", "计时", page == 0) { page = 0 }
            BottomAction("⌁", "联机", page == 1) { page = 1 }
        }
    }) { padding ->
        when (page) {
            0 -> TimerPage(viewModel, mode, { mode = it }, Modifier.padding(padding))
            else -> OnlinePage(bluetooth, Modifier.padding(padding))
        }
    }
}

@Composable
private fun TimerPage(viewModel: MainTimerViewModel, mode: Int, onMode: (Int) -> Unit, modifier: Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("DCTimer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainIconButton("⌂", "成绩")
                MainIconButton("⚙", "设置")
                MainIconButton("♢", "联机")
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(AppWhite), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("计时分组", color = Muted, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("3×3", "2×2", "盲拧", "自定义").forEachIndexed { index, label ->
                        FilterChip(selected = mode == index, onClick = { onMode(index) }, label = { Text(label) })
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(AppWhite), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("当前打乱", color = Muted, style = MaterialTheme.typography.labelMedium)
                Text("R U R' U' F' U' F", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = {}) { Text("上一条") }; OutlinedButton(onClick = {}) { Text("换一个") } }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).background(AppWhite, RoundedCornerShape(18.dp)).border(1.dp, Line, RoundedCornerShape(18.dp)).pointerInput(Unit) { detectTapGestures(onPress = { viewModel.press(); tryAwaitRelease(); viewModel.release() }) }, Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(timerLabel(state), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light); Text(phaseLabel(state.phase), color = Muted) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { OutlinedButton(onClick = { viewModel.cancel() }) { Text("重置") } }
    }
}

@Composable private fun MainIconButton(icon: String, label: String) { Surface(Modifier.size(44.dp).clickable { }, shape = RoundedCornerShape(12.dp), color = AppWhite, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(icon); Text(label, style = MaterialTheme.typography.labelSmall, color = Muted) } } }
@Composable private fun BottomAction(icon: String, label: String, selected: Boolean, onClick: () -> Unit) { Surface(Modifier.weight(1f).height(52.dp).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = if (selected) Color(0xFFF1F2F4) else AppWhite, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text(icon); Spacer(Modifier.width(8.dp)); Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } } }

@Composable private fun OnlinePage(controller: MutableState<BluetoothCubeController?>, modifier: Modifier) { Box(modifier.fillMaxSize().padding(18.dp), Alignment.Center) { Text("联机界面\n蓝牙设备列表将在此显示", textAlign = TextAlign.Center, color = Muted) } }
private fun phaseLabel(p: TimerState.Phase) = when (p) { TimerState.Phase.READY -> "准备就绪"; TimerState.Phase.INSPECTING -> "观察中"; TimerState.Phase.RUNNING -> "计时中"; TimerState.Phase.STOPPED -> "已停止" }
private fun timerLabel(s: TimerState) = if (s.phase == TimerState.Phase.READY) "0.000" else "%d.%03d".format(s.elapsedMs / 1000, s.elapsedMs % 1000)
