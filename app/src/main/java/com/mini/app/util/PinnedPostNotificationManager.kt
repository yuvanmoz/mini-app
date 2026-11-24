package com.mini.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mini.app.MainActivity
import com.mini.app.R
import com.mini.app.domain.model.Post

object PinnedPostNotificationManager {

    const val CHANNEL_ID = "pinned_post_channel"
    const val NOTIFICATION_ID = 1001
    private const val PREFS_NAME = "mini_pinned_post_prefs"
    private const val KEY_PINNED_TWEET_ID = "pinned_tweet_id"
    private const val KEY_AUTHOR_NAME = "pinned_author_name"
    private const val KEY_AUTHOR_HANDLE = "pinned_author_handle"
    private const val KEY_TEXT = "pinned_text"
    private const val KEY_TWEET_URL = "pinned_tweet_url"
    private const val KEY_TAG_NAME = "pinned_tag_name"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pinned Posts & Quotes"
            val descriptionText = "Displays your pinned tweet on the lock screen & notification shade"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        tweetId: String,
        authorDisplayName: String,
        authorHandle: String,
        text: String,
        tweetUrl: String,
        tagName: String?
    ): Notification {
        createNotificationChannel(context)

        // Content Intent (Open in Mini)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Intent.ACTION_VIEW
            data = Uri.parse(tweetUrl)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Unpin
        val unpinIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_UNPIN
        }
        val unpinPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            unpinIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Copy Text
        val copyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COPY
            putExtra("text", text)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Open on X
        val openXIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openXPendingIntent = PendingIntent.getActivity(
            context,
            3,
            openXIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "$authorDisplayName (@$authorHandle)"
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(text)
            .setSummaryText(if (!tagName.isNullOrBlank()) "#$tagName" else "Pinned Quote")

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pin)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(bigTextStyle)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true) // Sticky / Persistent
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on lock screen
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Unpin", unpinPendingIntent)
            .addAction(0, "Copy Text", copyPendingIntent)
            .addAction(0, "Open on X", openXPendingIntent)
            .build()
    }

    fun pinPost(context: Context, post: Post) {
        val tagName = post.tags.firstOrNull()?.name

        // Persist pinned state so it survives app closure and reboots
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PINNED_TWEET_ID, post.tweetId)
            .putString(KEY_AUTHOR_NAME, post.author.displayName)
            .putString(KEY_AUTHOR_HANDLE, post.author.handle)
            .putString(KEY_TEXT, post.text)
            .putString(KEY_TWEET_URL, post.tweetUrl)
            .putString(KEY_TAG_NAME, tagName)
            .apply()

        // Start Foreground Service to guarantee non-dismissible persistent notification on Android 14+
        val serviceIntent = Intent(context, PinnedPostService::class.java).apply {
            action = PinnedPostService.ACTION_PIN
            putExtra(PinnedPostService.EXTRA_TWEET_ID, post.tweetId)
            putExtra(PinnedPostService.EXTRA_AUTHOR_NAME, post.author.displayName)
            putExtra(PinnedPostService.EXTRA_AUTHOR_HANDLE, post.author.handle)
            putExtra(PinnedPostService.EXTRA_TEXT, post.text)
            putExtra(PinnedPostService.EXTRA_TWEET_URL, post.tweetUrl)
            putExtra(PinnedPostService.EXTRA_TAG_NAME, tagName)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for cases where startForegroundService might fail
            val notification = buildNotification(
                context = context,
                tweetId = post.tweetId,
                authorDisplayName = post.author.displayName,
                authorHandle = post.author.handle,
                text = post.text,
                tweetUrl = post.tweetUrl,
                tagName = tagName
            )
            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (se: SecurityException) {
                se.printStackTrace()
            }
        }
    }

    fun unpinPost(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Stop foreground service
        val serviceIntent = Intent(context, PinnedPostService::class.java).apply {
            action = PinnedPostService.ACTION_UNPIN
        }
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun restorePinnedPost(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tweetId = prefs.getString(KEY_PINNED_TWEET_ID, null) ?: return
        val authorName = prefs.getString(KEY_AUTHOR_NAME, "") ?: ""
        val authorHandle = prefs.getString(KEY_AUTHOR_HANDLE, "") ?: ""
        val text = prefs.getString(KEY_TEXT, "") ?: ""
        val tweetUrl = prefs.getString(KEY_TWEET_URL, "") ?: ""
        val tagName = prefs.getString(KEY_TAG_NAME, null)

        val serviceIntent = Intent(context, PinnedPostService::class.java).apply {
            action = PinnedPostService.ACTION_PIN
            putExtra(PinnedPostService.EXTRA_TWEET_ID, tweetId)
            putExtra(PinnedPostService.EXTRA_AUTHOR_NAME, authorName)
            putExtra(PinnedPostService.EXTRA_AUTHOR_HANDLE, authorHandle)
            putExtra(PinnedPostService.EXTRA_TEXT, text)
            putExtra(PinnedPostService.EXTRA_TWEET_URL, tweetUrl)
            putExtra(PinnedPostService.EXTRA_TAG_NAME, tagName)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPinnedTweetId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PINNED_TWEET_ID, null)
    }
}
