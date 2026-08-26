package com.mashuktamim.inhale

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Simple storage for paused apps, settings and per-app usage stats. */
object Prefs {
    private const val FILE = "pause_prefs"
    private const val KEY_TARGETS = "target_packages"
    private const val KEY_COUNTDOWN = "countdown_seconds"
    private const val KEY_BYPASS_MINUTES = "bypass_minutes"
    private const val KEY_STATS = "app_stats"
    private const val KEY_OVERRIDES = "app_overrides"
    private const val KEY_ONBOARDED = "onboarded"

    private const val KEY_THEME_MODE = "theme_mode"

    enum class ThemeMode {
        SYSTEM,
        DARK,
        LIGHT,
        AMOLED
    }

    const val MIN_COUNTDOWN = 3
    const val DEFAULT_COUNTDOWN = 5

    const val MIN_BYPASS_MINUTES = 1
    const val DEFAULT_BYPASS_MINUTES = 5

    data class Stats(
        val opens: Int = 0,
        val blocked: Int = 0,
        val openTimeMs: Long = 0L,
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getTargets(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_TARGETS, emptySet()) ?: emptySet()

    fun toggleTarget(context: Context, packageName: String) {
        val current = getTargets(context).toMutableSet()
        if (!current.add(packageName)) current.remove(packageName)
        prefs(context).edit().putStringSet(KEY_TARGETS, current).apply()
    }

    fun getCountdown(context: Context): Int {
        val value = prefs(context).getInt(KEY_COUNTDOWN, DEFAULT_COUNTDOWN)
        return if (value < MIN_COUNTDOWN) MIN_COUNTDOWN else value
    }

    fun setCountdown(context: Context, seconds: Int) {
        val clamped = if (seconds < MIN_COUNTDOWN) MIN_COUNTDOWN else seconds
        prefs(context).edit().putInt(KEY_COUNTDOWN, clamped).apply()
    }

    fun getBypassMinutes(context: Context): Int {
        val value = prefs(context).getInt(KEY_BYPASS_MINUTES, DEFAULT_BYPASS_MINUTES)
        return if (value < MIN_BYPASS_MINUTES) MIN_BYPASS_MINUTES else value
    }

    fun setBypassMinutes(context: Context, minutes: Int) {
        val clamped = if (minutes < MIN_BYPASS_MINUTES) MIN_BYPASS_MINUTES else minutes
        prefs(context).edit().putInt(KEY_BYPASS_MINUTES, clamped).apply()
    }

    fun isOnboarded(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDED, true).apply()
    }

    fun getThemeMode(context: Context): ThemeMode {
        val name = prefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    // --- Per-app stats ---

    private fun statsJson(context: Context): JSONObject =
        prefs(context).getString(KEY_STATS, null)?.let {
            try { JSONObject(it) } catch (e: Exception) { JSONObject() }
        } ?: JSONObject()

    private fun saveStats(context: Context, json: JSONObject) {
        prefs(context).edit().putString(KEY_STATS, json.toString()).apply()
    }

    fun getStats(context: Context, packageName: String): Stats {
        val entry = statsJson(context).optJSONObject(packageName) ?: return Stats()
        return Stats(
            opens = entry.optInt("opens"),
            blocked = entry.optInt("blocked"),
            openTimeMs = entry.optLong("openTimeMs"),
        )
    }

    fun getAllStats(context: Context): Map<String, Stats> {
        val json = statsJson(context)
        val result = mutableMapOf<String, Stats>()
        for (pkg in json.keys()) result[pkg] = getStats(context, pkg)
        return result
    }

    /** Called when the user actually opens the app ("Open anyway"). */
    fun recordOpen(context: Context, packageName: String) = mutateStats(context, packageName) {
        it.put("opens", it.optInt("opens") + 1)
    }

    /** Called when the user backs out of the pause screen without opening ("take me home"). */
    fun recordBlocked(context: Context, packageName: String) = mutateStats(context, packageName) {
        it.put("blocked", it.optInt("blocked") + 1)
    }

    /** Accumulates time the app spent in the foreground after a bypass. */
    fun addOpenTime(context: Context, packageName: String, ms: Long) = mutateStats(context, packageName) {
        it.put("openTimeMs", it.optLong("openTimeMs") + ms)
    }

    private inline fun mutateStats(context: Context, packageName: String, block: (JSONObject) -> Unit) {
        val json = statsJson(context)
        val entry = json.optJSONObject(packageName) ?: JSONObject()
        block(entry)
        json.put(packageName, entry)
        saveStats(context, json)
    }

    // --- Per-app duration overrides (null = use the global setting) ---

    fun getAppCountdown(context: Context, packageName: String): Int? {
        val entry = overridesJson(context).optJSONObject(packageName) ?: return null
        return if (entry.has("countdown")) entry.getInt("countdown") else null
    }

    fun setAppCountdown(context: Context, packageName: String, seconds: Int?) =
        mutateOverride(context, packageName) { entry ->
            if (seconds == null) entry.remove("countdown") else entry.put("countdown", seconds)
        }

    fun getAppBypassMinutes(context: Context, packageName: String): Int? {
        val entry = overridesJson(context).optJSONObject(packageName) ?: return null
        return if (entry.has("bypass")) entry.getInt("bypass") else null
    }

    fun setAppBypassMinutes(context: Context, packageName: String, minutes: Int?) =
        mutateOverride(context, packageName) { entry ->
            if (minutes == null) entry.remove("bypass") else entry.put("bypass", minutes)
        }

    /** Per-app pause duration, falling back to the global one. */
    fun getEffectiveCountdown(context: Context, packageName: String): Int =
        getAppCountdown(context, packageName) ?: getCountdown(context)

    /** Per-app bypass duration, falling back to the global one. */
    fun getEffectiveBypassMinutes(context: Context, packageName: String): Int =
        getAppBypassMinutes(context, packageName) ?: getBypassMinutes(context)

    private fun overridesJson(context: Context): JSONObject =
        prefs(context).getString(KEY_OVERRIDES, null)?.let {
            try { JSONObject(it) } catch (e: Exception) { JSONObject() }
        } ?: JSONObject()

    private inline fun mutateOverride(context: Context, packageName: String, block: (JSONObject) -> Unit) {
        val json = overridesJson(context)
        val entry = json.optJSONObject(packageName) ?: JSONObject()
        block(entry)
        if (entry.length() == 0) json.remove(packageName) else json.put(packageName, entry)
        prefs(context).edit().putString(KEY_OVERRIDES, json.toString()).apply()
    }
}
