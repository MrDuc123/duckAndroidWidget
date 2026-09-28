package com.benbo.ainews.data

import android.content.Context

/** App-wide widget settings, shared by the settings screen and the widget. */
data class WidgetSettings(
    /** "duck", "catbox", "icon" (a big faded category icon instead of a mascot) or "none". */
    val mascot: String,
    /** "peek" (small, in the corner) or "faded" (big and faded, behind the text). */
    val mascotStyle: String,
    /** null (the top story's category colour) or a colour like 0xFFEC4899: a preset or one picked with "+". */
    val color: Long?,
    /** When the stories were last refreshed (epoch millis). */
    val refreshedAt: Long,
)

object Prefs {
    private const val FILE = "ainews"
    private const val MASCOT = "mascot"
    private const val MASCOT_STYLE = "mascot_style"
    private const val COLOR = "color"
    private const val REFRESHED_AT = "refreshed_at"

    private fun sp(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(context: Context): WidgetSettings {
        val p = sp(context)
        if (!p.contains(REFRESHED_AT)) p.edit().putLong(REFRESHED_AT, System.currentTimeMillis()).apply()
        return WidgetSettings(
            mascot = p.getString(MASCOT, "duck") ?: "duck",
            mascotStyle = p.getString(MASCOT_STYLE, "peek") ?: "peek",
            color = if (p.contains(COLOR)) p.getLong(COLOR, 0L) else null,
            refreshedAt = p.getLong(REFRESHED_AT, System.currentTimeMillis()),
        )
    }

    fun setMascot(context: Context, mascot: String) = sp(context).edit().putString(MASCOT, mascot).apply()

    fun setMascotStyle(context: Context, style: String) = sp(context).edit().putString(MASCOT_STYLE, style).apply()

    fun setColor(context: Context, color: Long?) = sp(context).edit().apply {
        if (color == null) remove(COLOR) else putLong(COLOR, color)
    }.apply()

    fun markRefreshed(context: Context) = sp(context).edit().putLong(REFRESHED_AT, System.currentTimeMillis()).apply()

    /** For testing moods: make the top story look [minutes] old. */
    fun setTopStoryAge(context: Context, minutes: Int) {
        val top = SampleNews.stories.first().minutesBeforeRefresh
        val refreshedAt = System.currentTimeMillis() - (minutes - top) * 60_000L
        sp(context).edit().putLong(REFRESHED_AT, refreshedAt).apply()
    }

    /** Age in minutes of [story], given when the feed was refreshed. */
    fun ageOf(story: Story, settings: WidgetSettings, now: Long = System.currentTimeMillis()): Long =
        ((now - settings.refreshedAt) / 60_000L + story.minutesBeforeRefresh).coerceAtLeast(0)
}
