package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

/**
 * Resizable Home Screen AppWidget representing the Aura Capsule Player on the Android Launcher.
 */
class AuraCapsuleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, "Piche Tere") ?: "Piche Tere"
        val artist = prefs.getString(KEY_ARTIST, "Kunwarr, Dishant") ?: "Kunwarr, Dishant"
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)

        for (appWidgetId in appWidgetIds) {
            updateWidgetInstance(context, appWidgetManager, appWidgetId, title, artist, isPlaying)
        }
    }

    companion object {
        private const val PREFS_NAME = "aura_widget_prefs"
        private const val KEY_TITLE = "widget_track_title"
        private const val KEY_ARTIST = "widget_track_artist"
        private const val KEY_IS_PLAYING = "widget_is_playing"

        fun updateAllWidgets(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean
        ) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .putBoolean(KEY_IS_PLAYING, isPlaying)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AuraCapsuleWidgetProvider::class.java))
            for (id in ids) {
                updateWidgetInstance(context, manager, id, title, artist, isPlaying)
            }
        }

        private fun updateWidgetInstance(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String,
            artist: String,
            isPlaying: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_aura_capsule)
            views.setTextViewText(R.id.widget_track_title, title)
            views.setTextViewText(R.id.widget_track_artist, artist)
            views.setTextViewText(
                R.id.widget_status_badge,
                if (isPlaying) "▮▮ PLAYING" else "▶ PAUSED"
            )

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
