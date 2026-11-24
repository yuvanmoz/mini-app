package com.mini.app.util

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class PinnedPostService : Service() {

    companion object {
        const val ACTION_PIN = "com.mini.app.ACTION_PIN_POST_SERVICE"
        const val ACTION_UNPIN = "com.mini.app.ACTION_UNPIN_POST_SERVICE"

        const val EXTRA_TWEET_ID = "extra_tweet_id"
        const val EXTRA_AUTHOR_NAME = "extra_author_name"
        const val EXTRA_AUTHOR_HANDLE = "extra_author_handle"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_TWEET_URL = "extra_tweet_url"
        const val EXTRA_TAG_NAME = "extra_tag_name"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PIN -> {
                val tweetId = intent.getStringExtra(EXTRA_TWEET_ID) ?: ""
                val authorName = intent.getStringExtra(EXTRA_AUTHOR_NAME) ?: ""
                val authorHandle = intent.getStringExtra(EXTRA_AUTHOR_HANDLE) ?: ""
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                val tweetUrl = intent.getStringExtra(EXTRA_TWEET_URL) ?: ""
                val tagName = intent.getStringExtra(EXTRA_TAG_NAME)

                val notification = PinnedPostNotificationManager.buildNotification(
                    context = this,
                    tweetId = tweetId,
                    authorDisplayName = authorName,
                    authorHandle = authorHandle,
                    text = text,
                    tweetUrl = tweetUrl,
                    tagName = tagName
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                    startForeground(PinnedPostNotificationManager.NOTIFICATION_ID, notification, serviceType)
                } else {
                    startForeground(PinnedPostNotificationManager.NOTIFICATION_ID, notification)
                }

                return START_STICKY
            }
            ACTION_UNPIN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_NOT_STICKY
    }
}
