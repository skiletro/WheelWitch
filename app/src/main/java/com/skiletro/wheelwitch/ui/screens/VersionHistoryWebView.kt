package com.skiletro.wheelwitch.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VersionHistoryWebView(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            try {
                WebView(context).apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                    settings.javaScriptEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            if (url.startsWith("https://wiki.tockdom.com/wiki/Retro_Rewind")) {
                                return false
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            return true
                        }
                    }
                    loadUrl("https://wiki.tockdom.com/wiki/Retro_Rewind#Version_History")
                }
            } catch (_: Exception) {
                android.widget.TextView(context).apply {
                    text = "Version history unavailable"
                    gravity = android.view.Gravity.CENTER
                    textSize = 14f
                }
            }
        },
        modifier = modifier
    )
}
