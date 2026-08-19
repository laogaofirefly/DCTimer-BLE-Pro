package com.example.lanmultiplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: LanViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val match by viewModel.match.collectAsStateWithLifecycle()
                    LaunchedEffect(match.active) {
                        if (match.active) {
                            startActivity(Intent(this@MainActivity, com.dctimerble.pro.activity.MainActivity::class.java))
                        }
                    }
                    if (!match.active) LanScreen(viewModel)
                }
            }
        }
    }
}