package com.mini.app.widget

import android.content.Context

object WidgetPreferences {
    private const val PREFS_NAME = "mini_widget_prefs"
    private const val PREF_PREFIX_KEY = "appwidget_"

    fun savePostForWidget(context: Context, appWidgetId: Int, tweetId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("$PREF_PREFIX_KEY$appWidgetId", tweetId).apply()
    }

    fun getPostIdForWidget(context: Context, appWidgetId: Int): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("$PREF_PREFIX_KEY$appWidgetId", null)
    }

    fun removeWidget(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$PREF_PREFIX_KEY$appWidgetId").apply()
    }

    fun getAllWidgetIds(context: Context): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.keys
            .filter { it.startsWith(PREF_PREFIX_KEY) }
            .mapNotNull { it.removePrefix(PREF_PREFIX_KEY).toIntOrNull() }
    }
}
