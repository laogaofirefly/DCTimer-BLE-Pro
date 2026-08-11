package com.dctimerble.pro.timer

import android.app.Activity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object MainComposeUi {
    @JvmStatic fun install(activity: Activity) {
        activity.setContent { MaterialTheme { MainComposeRoot() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainComposeRoot(viewModel: MainTimerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    val solves = remember { mutableStateListOf<SolveRecord>() }
    Scaffold(
        topBar = { TopAppBar(title = { Text(if (tab == 0) "DCTimer" else if (tab == 1) "成绩" else "设置") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Timer, null) }, label = { Text("计时") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("成绩") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("设置") })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> TimerPage(viewModel, Modifier.padding(padding))
            1 -> ResultsPage(solves, Modifier.padding(padding))
            else -> SettingsPage(Modifier.padding(padding))
        }
    }
}

@Composable
private fun TimerPage(viewModel: MainTimerViewModel, modifier: Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("当前打乱", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("R U R' U' F' U' F", style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = {}) { Text("上一条") }
                    OutlinedButton(onClick = {}) { Text("换一个") }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
                .pointerInput(Unit) { detectTapGestures(onPress = { viewModel.press(); tryAwaitRelease(); viewModel.release() }) },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(timerLabel(state), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light)
                Text(phaseLabel(state.phase), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.penalty != TimerState.Penalty.NONE) Text(state.penalty.name, color = MaterialTheme.colorScheme.error)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("按住计时区域开始，松开停止", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(12.dp))
            Button(onClick = { viewModel.cancel() }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("重置") }
        }
    }
}

@Composable
private fun ResultsPage(solves: List<SolveRecord>, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("暂无成绩\n完成一次计时后会显示在这里", textAlign = TextAlign.Center)
    }
}

@Composable
private fun SettingsPage(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingRow("WCA 检查", "关闭")
        SettingRow("计时精度", "0.001 秒")
        SettingRow("声音与震动", "跟随系统")
        SettingRow("主题", "跟随系统")
    }
}

@Composable private fun SettingRow(title: String, value: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(title); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun phaseLabel(phase: TimerState.Phase) = when (phase) {
    TimerState.Phase.READY -> "准备就绪"
    TimerState.Phase.INSPECTING -> "观察中"
    TimerState.Phase.RUNNING -> "计时中"
    TimerState.Phase.STOPPED -> "已停止"
}
private fun timerLabel(state: TimerState) = if (state.phase == TimerState.Phase.READY) "0.000" else "%d.%03d".format(state.elapsedMs / 1000, state.elapsedMs % 1000)
