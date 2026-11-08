package com.jb.viso

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var fileCallback: ValueCallback<Array<android.net.Uri>>? = null

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            fileCallback?.onReceiveValue(uris.toTypedArray())
            fileCallback = null
        }

    private val perms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askPermissions()
        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> WebView(ctx).apply { webView = this; setup() } }
            )
        }
    }

    private fun askPermissions() {
        val list = if (Build.VERSION.SDK_INT >= 33) arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        ) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        perms.launch(list)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.setup() {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        addJavascriptInterface(AndroidSave(this@MainActivity), "AndroidSave")

        webViewClient = WebViewClient()
        webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<android.net.Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback = callback
                val types = params?.acceptTypes ?: arrayOf("*/*")
                filePicker.launch(if (types.isEmpty()) arrayOf("*/*") else types)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
        loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else super.onBackPressed()
    }
}