package com.mini.app.util

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UNPIN = "com.mini.app.ACTION_UNPIN_POST"
        const val ACTION_COPY = "com.mini.app.ACTION_COPY_POST"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_UNPIN -> {
                PinnedPostNotificationManager.unpinPost(context)
                Toast.makeText(context, "Unpinned from notifications", Toast.LENGTH_SHORT).show()
            }
            ACTION_COPY -> {
                val text = intent.getStringExtra("text") ?: return
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Tweet Text", text))
                Toast.makeText(context, "Copied post text", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
