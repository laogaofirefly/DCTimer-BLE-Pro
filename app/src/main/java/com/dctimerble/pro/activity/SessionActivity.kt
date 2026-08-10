package com.dctimerble.pro.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dctimerble.pro.APP
import com.dctimerble.pro.database.SessionManager

/** Compose session screen. Persistence and smart-cube Java code remain unchanged. */
class SessionActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = APP.getInstance().sessionManager
        setContent {
            MaterialTheme {
                SessionScreen(sessionManager, APP.sessionIdx) { selected, changed ->
                    APP.sessionIdx = selected
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra("mod", changed)
                        putExtra("select", selected)
                    })
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionScreen(manager: SessionManager, initialSelection: Int, onFinish: (Int, Boolean) -> Unit) {
    var selected by remember { mutableIntStateOf(initialSelection.coerceIn(0, (manager.sessionLength - 1).coerceAtLeast(0))) }
    var revision by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var changed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sessions") }, navigationIcon = {
                TextButton(onClick = { onFinish(selected, changed) }) { Text("Back") }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        @Suppress("UNUSED_VARIABLE") val refresh = revision
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed((0 until manager.sessionLength).toList(), key = { _, index -> manager.getSession(index).id }) { index, _ ->
                val session = manager.getSession(index)
                Card(Modifier.fillMaxWidth().clickable { selected = index }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(manager.getSessionName(index), style = MaterialTheme.typography.titleMedium)
                            Text("${session.count} solves", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (index == selected) Icon(Icons.Default.RadioButtonChecked, "Selected", tint = MaterialTheme.colorScheme.primary)
                        if (index > 0) IconButton(onClick = { deleteIndex = index }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add session") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) },
            confirmButton = { Button(onClick = { manager.addSession(name.trim()); changed = true; showAdd = false; revision++ }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    deleteIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { deleteIndex = null },
            title = { Text("Delete session?") },
            text = { Text(manager.getSessionName(index)) },
            confirmButton = { Button(onClick = {
                manager.removeSession(index)
                if (selected >= manager.sessionLength) selected = (manager.sessionLength - 1).coerceAtLeast(0)
                changed = true; deleteIndex = null; revision++
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteIndex = null }) { Text("Cancel") } }
        )
    }
}
