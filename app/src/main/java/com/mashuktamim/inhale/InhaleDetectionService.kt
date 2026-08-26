package com.mashuktamim.inhale

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Watches for window-state changes (app switches). When a selected app comes to
 * the foreground, launches [InhaleActivity] on top of it.
 *
 * After the user acknowledges the pause screen ("Open anyway"), the package is
 * allowed for a configurable time window (bypass duration) — so navigating
 * inside the app or switching away and back does NOT re-trigger the pause.
 */
class InhaleDetectionService : AccessibilityService() {

    companion object {
        private val allowedUntil = mutableMapOf<String, Long>()

        /** Marks [packageName] as allowed (no pause screen) for [windowMs]. */
        fun allowPackage(packageName: String, windowMs: Long) {
            allowedUntil[packageName] = System.currentTimeMillis() + windowMs
        }

        fun isAllowed(packageName: String): Boolean {
            val until = allowedUntil[packageName] ?: return false
            if (System.currentTimeMillis() > until) {
                allowedUntil.remove(packageName)
                return false
            }
            return true
        }

        /** System/IME packages that should never trigger the pause screen. */
        val SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
        )
    }

    /** Throttle so one app switch doesn't fire multiple events. */
    private var lastPackageName: String? = null
    private var lastEventAt = 0L

    /** Package currently in foreground that is being timed (bypassed target). */
    private var timedPackage: String? = null
    private var timedSince = 0L

    private fun closeTimedSession() {
        val pkg = timedPackage ?: return
        val elapsed = System.currentTimeMillis() - timedSince
        if (elapsed > 0) Prefs.addOpenTime(this, pkg, elapsed)
        timedPackage = null
        timedSince = 0L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        // Ignore our own package and non-app windows (keyboard, launcher, system UI).
        if (pkg == packageName) return
        if (pkg in SYSTEM_PACKAGES) return

        // If a different app comes to the front, stop timing the previous one.
        if (pkg != timedPackage) closeTimedSession()

        // Same package re-reported within 2s — ignore.
        if (pkg == lastPackageName && now - lastEventAt < 2_000) return
        lastPackageName = pkg
        lastEventAt = now

        if (isAllowed(pkg)) {
            // Inside the bypass window: don't pause, and track usage time.
            if (timedPackage == null) {
                timedPackage = pkg
                timedSince = now
            }
            return
        }

        if (pkg in Prefs.getTargets(this)) {
            closeTimedSession()
            val intent = Intent(this, InhaleActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(InhaleActivity.EXTRA_TARGET_PACKAGE, pkg)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        closeTimedSession()
    }
}
