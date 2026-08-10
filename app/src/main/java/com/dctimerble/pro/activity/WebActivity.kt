package com.dctimerble.pro.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dctimerble.pro.APP

/** First Compose-migrated screen. BLE and cube protocol code remains Java. */
class WebActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("web").orEmpty()
        val title = intent.getStringExtra("title") ?: ""
        setContent {
            MaterialTheme {
                WebScreen(title = title, url = url, onBack = ::finish)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebScreen(title: String, url: String, onBack: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineLarge) }
                },
                actions = {
                    IconButton(onClick = { webView?.goBack() }) { Text("‹") }
                    IconButton(onClick = { webView?.reload() }) { Text("↻") }
                    IconButton(onClick = { webView?.goForward() }) { Text("›") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(APP.getBackgroundColor()),
                    titleContentColor = Color(APP.getTextColor())
                )
            )
        }
    ) { _ ->
        Column(Modifier.fillMaxSize()) {
            if (progress in 0f..0.99f) LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(2.dp))
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context -> WebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.builtInZoomControls = false
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) = handler.proceed()
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress / 100f }
                    }
                    webView = this
                    if (url.isNotBlank()) loadUrl(url)
                } },
                update = { webView = it }
            )
        }
    }
}
