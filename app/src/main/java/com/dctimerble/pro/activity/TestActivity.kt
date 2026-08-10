package com.dctimerble.pro.activity

import android.app.Activity
import android.media.AudioFormat
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dctimerble.pro.APP
import com.dctimerble.pro.model.Stackmat

/** Stackmat test UI migrated to Compose; Stackmat protocol/model remains Java. */
class TestActivity : AppCompatActivity() {
    private var stackmat: Stackmat? = null
    private var wave by mutableStateOf<List<Int>>(emptyList())
    private var status by mutableStateOf("OFF")
    private var time by mutableStateOf("0.000")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TestScreen(
                    samplingRate = APP.samplingRate,
                    format = APP.dataFormat,
                    status = status,
                    time = time,
                    wave = wave,
                    onRateChanged = ::setSamplingRate,
                    onFormatChanged = ::setFormat,
                    onBack = ::finish
                )
            }
        }
        startStackmat()
    }

    private fun startStackmat() {
        stackmat = Stackmat(this, APP.samplingRate, APP.dataFormat).also { it.start() }
    }

    private fun setSamplingRate(rate: Int) {
        if (rate == APP.samplingRate) return
        APP.samplingRate = rate
        getSharedPreferences("dctimer", Activity.MODE_PRIVATE).edit().putInt("srate", rate).apply()
        stackmat?.stop()
        startStackmat()
    }

    private fun setFormat(format: Int) {
        if (format == APP.dataFormat) return
        APP.dataFormat = format
        getSharedPreferences("dctimer", Activity.MODE_PRIVATE).edit().putInt("dform", format).apply()
        stackmat?.stop()
        startStackmat()
    }

    override fun onDestroy() {
        stackmat?.stop()
        stackmat = null
        super.onDestroy()
    }

    // Callbacks consumed by the unchanged Java Stackmat decoder.
    fun drawWave(data: Array<Int>) {
        wave = data.toList()
    }

    fun displayTime(newStatus: Int, text: String) {
        status = when (newStatus.toChar()) {
            'L' -> "LEFT"
            'R' -> "RIGHT"
            'C' -> "CONNECTED"
            'A' -> "READY"
            else -> "OFF"
        }
        time = text
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestScreen(
    samplingRate: Int,
    format: Int,
    status: String,
    time: String,
    wave: List<Int>,
    onRateChanged: (Int) -> Unit,
    onFormatChanged: (Int) -> Unit,
    onBack: () -> Unit
) {
    val rates = listOf(8000, 11025, 16000, 22050, 24000, 32000, 44100)
    Scaffold(topBar = {
        TopAppBar(title = { Text("Stackmat Test") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
    }) { padding ->
    val waveColor = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Selector("Sampling rate", samplingRate.toString(), rates.map(Int::toString), { onRateChanged(it.toInt()) }, Modifier.weight(1f))
                Selector("Format", if (format == AudioFormat.ENCODING_PCM_8BIT) "8 bit" else "16 bit", listOf("8 bit", "16 bit"), { onFormatChanged(if (it == "8 bit") AudioFormat.ENCODING_PCM_8BIT else AudioFormat.ENCODING_PCM_16BIT) }, Modifier.weight(1f))
            }
            Text("Status: $status", style = MaterialTheme.typography.titleMedium)
            Text("Time: $time", style = MaterialTheme.typography.headlineMedium)
            Waveform(wave, Modifier.fillMaxWidth().height(180.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Selector(label: String, value: String, values: List<String>, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(value, {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor())
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { onSelected(item); expanded = false }) }
        }
    }
}

@Composable
private fun Waveform(data: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF202124))
        if (data.size > 1) {
            val step = size.width / (data.size - 1)
            for (i in 1 until data.size) {
                drawLine(Color(0xFF66BB6A), Offset((i - 1) * step, data[i - 1] / 255f * size.height), Offset(i * step, data[i] / 255f * size.height), strokeWidth = 2f)
            }
        }
    }
}