package com.jb.viso

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.File

class AndroidSave(private val activity: ComponentActivity) {

    @JavascriptInterface
    fun save(base64: String, name: String) {
        Thread {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val mime = when {
                    name.endsWith(".png") -> "image/png"
                    name.endsWith(".webm") -> "video/webm"
                    else -> "application/octet-stream"
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }
                val uri = activity.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                if (uri != null) {
                    activity.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Saved: $name", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    File(dir, name).writeBytes(bytes)
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Saved: $name", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}