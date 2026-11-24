package com.mini.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.mini.app.MainActivity
import com.mini.app.R
import com.mini.app.data.local.AppDatabase
import com.mini.app.data.repository.toDomainPost
import com.mini.app.domain.model.Post
import com.mini.app.ui.widget.PostWidgetConfigureActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun appDatabase(): AppDatabase
}

class PostWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetPreferences.removeWidget(context, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PostWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.widget_post_layout)
                val tweetId = WidgetPreferences.getPostIdForWidget(context, appWidgetId)

                val post: Post? = if (!tweetId.isNullOrBlank()) {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            WidgetEntryPoint::class.java
                        )
                        entryPoint.appDatabase().postDao().getPostWithDetailsById(tweetId)?.toDomainPost()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    renderWidget(context, views, appWidgetId, post)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                }
            }
        }

        private fun renderWidget(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            post: Post?
        ) {
            // Configure Intent for Selecting / Switching Post
            val configureIntent = Intent(context, PostWidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val configurePendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 1,
                configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_btn_switch, configurePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_btn, configurePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_container, configurePendingIntent)

            // ALWAYS set the RemoteAdapter — launcher requires a fully-formed RemoteViews graph
            // at placement time. PostRemoteViewsFactory.getCount() returns 0 when no post is set.
            val serviceIntent = Intent(context, PostWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)

            // PendingIntent Template for clicks inside the scrollable ListView item
            val itemClickIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val itemClickPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 3,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)

            if (post == null) {
                // Show Empty / Select Post state
                views.setViewVisibility(R.id.widget_post_container, View.GONE)
                views.setViewVisibility(R.id.widget_empty_container, View.VISIBLE)
                return
            }

            // Show Active Post Container
            views.setViewVisibility(R.id.widget_empty_container, View.GONE)
            views.setViewVisibility(R.id.widget_post_container, View.VISIBLE)

            // Header Author Info
            views.setTextViewText(R.id.widget_author_name, post.author.displayName)
            views.setTextViewText(R.id.widget_author_handle, "@${post.author.handle}")

            // Author Avatar loading
            val avatarBitmap = loadCircleAvatarBitmap(context, post.author.avatarUrl, post.author.displayName)
            if (avatarBitmap != null) {
                views.setImageViewBitmap(R.id.widget_author_avatar, avatarBitmap)
            } else {
                views.setImageViewResource(R.id.widget_author_avatar, R.drawable.app_icon)
            }

            // Open Post in App action button
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(post.tweetUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 2,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_open, openPendingIntent)
        }

        fun loadCircleAvatarBitmap(context: Context, avatarPath: String?, displayName: String): Bitmap? {
            try {
                if (!avatarPath.isNullOrBlank()) {
                    val file = File(avatarPath)
                    if (file.exists()) {
                        val src = BitmapFactory.decodeFile(file.absolutePath)
                        if (src != null) {
                            return getCircularBitmap(src, 100)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return createInitialAvatarBitmap(displayName, 100)
        }

        fun getCircularBitmap(src: Bitmap, size: Int): Bitmap {
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val minDim = Math.min(src.width, src.height)
            val srcRect = Rect((src.width - minDim) / 2, (src.height - minDim) / 2, (src.width + minDim) / 2, (src.height + minDim) / 2)

            paint.shader = BitmapShader(
                Bitmap.createScaledBitmap(
                    Bitmap.createBitmap(src, srcRect.left, srcRect.top, srcRect.width(), srcRect.height()),
                    size, size, true
                ),
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            return output
        }

        fun createInitialAvatarBitmap(name: String, size: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#338B5CF6")
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#808B5CF6")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawCircle(size / 2f, size / 2f, (size - 3) / 2f, bgPaint)
            canvas.drawCircle(size / 2f, size / 2f, (size - 3) / 2f, borderPaint)

            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "M"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8B5CF6")
                textSize = size * 0.45f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textBounds = Rect()
            textPaint.getTextBounds(initial, 0, initial.length, textBounds)
            canvas.drawText(initial, size / 2f, size / 2f + textBounds.height() / 2f, textPaint)
            return bitmap
        }

        fun loadRoundedMediaBitmap(path: String, targetW: Int, targetH: Int, cornerRadius: Float): Bitmap? {
            try {
                val file = File(path)
                if (!file.exists()) return null

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)

                var inSampleSize = 1
                val height = options.outHeight
                val width = options.outWidth
                if (height > targetH || width > targetW) {
                    val halfHeight = height / 2
                    val halfWidth = width / 2
                    while ((halfHeight / inSampleSize) >= targetH && (halfWidth / inSampleSize) >= targetW) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

                // Round corners
                val output = Bitmap.createBitmap(rawBitmap.width, rawBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rect = RectF(0f, 0f, rawBitmap.width.toFloat(), rawBitmap.height.toFloat())
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(rawBitmap, 0f, 0f, paint)
                rawBitmap.recycle()
                return output
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}
