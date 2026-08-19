package com.example.lanmultiplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: LanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanMatchBridge.bindLanActivity(this)
        requestLanPermissions()
        setContent {
            MaterialTheme {
                Surface {
                    val match by viewModel.match.collectAsStateWithLifecycle()
                    if (!match.active) LanScreen(viewModel) else LanMatchScreen(viewModel)
                }
            }
        }
    }

    private fun requestLanPermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
            if (Build.VERSION.SDK_INT >= 23) add(Manifest.permission.ACCESS_FINE_LOCATION)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), 4101)
    }
}