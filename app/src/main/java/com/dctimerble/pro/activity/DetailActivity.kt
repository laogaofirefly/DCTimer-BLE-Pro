package com.dctimerble.pro.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dctimerble.pro.APP
import com.dctimerble.pro.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Statistics detail screen migrated to Compose. Export still uses Android SAF. */
class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val avg = intent.getIntExtra("avg", 0)
        val pos = intent.getIntExtra("pos", 0)
        val len = intent.getIntExtra("len", 0)
        val stat = intent.getStringArrayExtra("detail")?.toList().orEmpty()
        setContent {
            MaterialTheme {
                DetailScreen(
                    stat = stat,
                    avg = avg,
                    pos = pos,
                    len = len,
                    onBack = ::finish,
                    onCopy = { copyStats() },
                    onExport = { fileName -> exportStats(fileName) }
                )
            }
        }
    }

    private fun copyStats() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("text", APP.statDetail))
        Toast.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
    }

    private fun exportStats(fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, ensureFileName(fileName))
        }
        exportLauncher.launch(intent)
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) result.data?.data?.let { uri: Uri ->
            com.dctimerble.pro.util.Utils.saveStat(this, uri, APP.statDetail)
        }
    }

    private fun ensureFileName(name: String): String {
        val fallback = "stats_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val value = name.trim().ifEmpty { fallback }
        return if (value.endsWith(".txt", true)) value else "$value.txt"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    stat: List<String>,
    avg: Int,
    pos: Int,
    len: Int,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onExport: (String) -> Unit
) {
    var showExport by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "Copy") }
                    IconButton(onClick = { showExport = true }) { Icon(Icons.Default.Save, "Save") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            Text("Average: $avg    Range: ${if (len > 0) "${pos + 1}-$len" else "-"}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(stat) { index, value ->
                    Card(Modifier.fillMaxWidth()) { Text("${index + 1}. $value", Modifier.padding(12.dp)) }
                }
            }
        }
    }
    if (showExport) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("Save statistics") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("File name") }, singleLine = true) },
            confirmButton = { Button(onClick = { onExport(name); showExport = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showExport = false }) { Text("Cancel") } }
        )
    }
}