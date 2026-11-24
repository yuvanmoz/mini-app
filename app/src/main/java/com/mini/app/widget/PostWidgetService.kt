package com.mini.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.mini.app.R
import com.mini.app.data.repository.toDomainPost
import com.mini.app.domain.model.Post
import com.mini.app.ui.feed.formatRelativeTime
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import java.io.File

class PostWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PostRemoteViewsFactory(this.applicationContext, intent)
    }
}

class PostRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var currentPost: Post? = null

    override fun onCreate() {}

    override fun onDataSetChanged() {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val tweetId = WidgetPreferences.getPostIdForWidget(context, appWidgetId)
        if (tweetId.isNullOrBlank()) {
            currentPost = null
            return
        }

        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            runBlocking {
                currentPost = entryPoint.appDatabase().postDao().getPostWithDetailsById(tweetId)?.toDomainPost()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentPost = null
        }
    }

    override fun onDestroy() {
        currentPost = null
    }

    override fun getCount(): Int {
        return if (currentPost != null) 1 else 0
    }

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_post_item)
        val post = currentPost ?: return views

        views.setTextViewText(R.id.item_post_text, post.text)
        views.setTextViewText(R.id.item_date_text, formatRelativeTime(post.savedAtMs))

        // Tag pill
        val primaryTag = post.tags.firstOrNull()
        if (primaryTag != null && primaryTag.name.isNotBlank()) {
            views.setTextViewText(R.id.item_tag_pill, "#${primaryTag.name}")
            views.setViewVisibility(R.id.item_tag_pill, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.item_tag_pill, View.GONE)
        }

        // Media image
        val primaryMedia = post.mediaItems.firstOrNull()
        val mediaPath = primaryMedia?.localPath
        if (!mediaPath.isNullOrBlank() && File(mediaPath).exists()) {
            val bitmap = PostWidgetProvider.loadRoundedMediaBitmap(mediaPath, 600, 320, 18f)
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.item_post_image, bitmap)
                views.setViewVisibility(R.id.item_post_image, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.item_post_image, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.item_post_image, View.GONE)
        }

        // Fill-in intent for item click
        val fillInIntent = Intent().apply {
            data = Uri.parse(post.tweetUrl)
        }
        views.setOnClickFillInIntent(R.id.item_root, fillInIntent)
        views.setOnClickFillInIntent(R.id.item_post_text, fillInIntent)
        views.setOnClickFillInIntent(R.id.item_post_image, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
