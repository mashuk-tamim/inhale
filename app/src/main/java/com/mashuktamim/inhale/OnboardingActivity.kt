package com.mashuktamim.inhale

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * First-launch onboarding: grants necessary background execution & accessibility permissions.
 */
class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Prefs.isOnboarded(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val themeMode = Prefs.getThemeMode(this)

        setContent {
            InhaleAppTheme(themeMode = themeMode) {
                var step by remember { mutableIntStateOf(0) }
                val ctx = LocalContext.current

                val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, step) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            if (step == 0 && isAccessibilityGranted(ctx)) step = 1
                            if (step == 1 && UsageTracker.hasUsageAccess(ctx)) step = 2
                            if (step == 2 && isIgnoringBatteryOptimizations(ctx)) step = 3
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                when (step) {
                    0 -> PermissionScreen(
                        progress = 0.33f,
                        title = "Accessibility Access",
                        icon = Icons.Rounded.AccessibilityNew,
                        body = "Inhale uses accessibility access to detect when you launch a paused app and present the breathing exercise.",
                        steps = listOf(
                            "Tap \"Open Settings\" below",
                            "Locate \"Inhale\" in the list",
                            "Toggle the switch to Enable"
                        ),
                        buttonText = "Open Settings",
                        onSkip = null,
                        onAction = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        onNext = { step = 1 }
                    )
                    1 -> PermissionScreen(
                        progress = 0.66f,
                        title = "Usage Access",
                        icon = Icons.Rounded.DataUsage,
                        body = "Grant usage access so Inhale can show real screen-time insights for each app.",
                        steps = listOf(
                            "Tap \"Open Settings\" below",
                            "Find \"Inhale\" in Usage access",
                            "Allow usage tracking"
                        ),
                        buttonText = "Open Settings",
                        onSkip = { step = 2 },
                        onAction = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                    2 -> PermissionScreen(
                        progress = 1f,
                        title = "Background Continuity",
                        icon = Icons.Rounded.BatteryChargingFull,
                        body = "To prevent Android from halting Inhale in the background, allow it to run without battery restrictions.",
                        steps = emptyList(),
                        buttonText = "Disable Battery Optimization",
                        onSkip = { step = 3 },
                        onAction = {
                            try {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                        .setData(Uri.parse("package:${ctx.packageName}"))
                                )
                            } catch (e: Exception) {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                            }
                        }
                    )
                    else -> {
                        LaunchedEffect(Unit) {
                            Prefs.setOnboarded(ctx)
                        }
                        WelcomeScreen(
                            onStart = {
                                startActivity(Intent(ctx, MainActivity::class.java))
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun WelcomeScreen(onStart: () -> Unit) {
        val colors = InhaleTheme.colors

        PermissionShell(progress = 1f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                PermissionIcon(Icons.Rounded.Spa, colors)
                Spacer(Modifier.height(24.dp))
                Text(
                    "You're all set",
                    style = InhaleTheme.typography.displayMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Select distracting apps to pause, and take a mindful breath before opening them.",
                    style = InhaleTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(36.dp))
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        "Get Started",
                        style = InhaleTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    @Composable
    private fun PermissionScreen(
        progress: Float,
        title: String,
        icon: ImageVector,
        body: String,
        steps: List<String>,
        buttonText: String,
        onSkip: (() -> Unit)?,
        onAction: () -> Unit,
        onNext: (() -> Unit)? = null
    ) {
        val colors = InhaleTheme.colors
        val ctx = LocalContext.current
        val isAccessibility = title.startsWith("Accessibility")

        PermissionShell(progress = progress) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                Spacer(Modifier.weight(0.5f))
                PermissionIcon(icon, colors)
                Spacer(Modifier.height(22.dp))
                Text(
                    title,
                    style = InhaleTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    body,
                    style = InhaleTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                if (steps.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            steps.forEachIndexed { i, s ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            "${i + 1}",
                                            style = InhaleTheme.typography.labelSmall,
                                            color = colors.primary
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        s,
                                        style = InhaleTheme.typography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        buttonText,
                        style = InhaleTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (onSkip != null) {
                    TextButton(onClick = onSkip) {
                        Text(
                            "Skip for now",
                            style = InhaleTheme.typography.labelMedium,
                            color = colors.textTertiary
                        )
                    }
                }

                if (isAccessibility) {
                    var granted by remember { mutableStateOf(isAccessibilityGranted(ctx)) }
                    val lifecycleOwner2 = androidx.compose.ui.platform.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner2) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                granted = isAccessibilityGranted(ctx)
                            }
                        }
                        lifecycleOwner2.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner2.lifecycle.removeObserver(observer) }
                    }
                    TextButton(onClick = { if (granted) onNext?.invoke() }) {
                        Text(
                            if (granted) "Continue" else "I've enabled it",
                            style = InhaleTheme.typography.labelMedium,
                            color = if (granted) colors.primary else colors.textTertiary
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PermissionShell(progress: Float, content: @Composable () -> Unit) {
        val colors = InhaleTheme.colors
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding)
            ) {
                content()
            }

            // Minimalist Progress Bar
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(colors.borderSubtle)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(colors.primary)
                )
            }
        }
    }

    @Composable
    private fun PermissionIcon(icon: ImageVector, colors: InhaleColors) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(BorderStroke(1.dp, colors.borderSubtle), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    private fun isAccessibilityGranted(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == ctx.packageName }
    }

    private fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }
}
