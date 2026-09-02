package com.mashuktamim.inhale

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context

/** Reads real screen time per app from the system (needs "Usage access" permission). */
object UsageTracker {

    /** Whether the user granted this app access to usage statistics. */
    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Foreground time in ms per package over the last 24 hours. */
    fun getUsageLast24h(context: Context): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, Long>()
        for ((pkg, stats) in usm.queryAndAggregateUsageStats(now - 24L * 60 * 60 * 1000, now)) {
            val time = stats.totalTimeInForeground
            if (time > 0) result[pkg] = time
        }
        return result
    }

    /** Foreground time in ms for one package, one value per day for the last [days] days (oldest first). */
    fun getUsagePerDay(context: Context, pkg: String, days: Int): List<Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val endOfToday = cal.timeInMillis + 24L * 60 * 60 * 1000
        return (days - 1 downTo 0).map { back ->
            val end = endOfToday - back * 24L * 60 * 60 * 1000
            val start = end - 24L * 60 * 60 * 1000
            usm.queryAndAggregateUsageStats(start, end)[pkg]?.totalTimeInForeground ?: 0L
        }
    }
}
