package com.mashuktamim.inhale

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Global settings: appearance/theme, default pause/bypass durations, and permission diagnostics.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(Prefs.getThemeMode(context)) }

            InhaleAppTheme(themeMode = themeMode) {
                SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        Prefs.setThemeMode(context, it)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    themeMode: Prefs.ThemeMode,
    onThemeModeChange: (Prefs.ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val colors = InhaleTheme.colors
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var countdown by remember { mutableStateOf(Prefs.getCountdown(context)) }
    var bypassMinutes by remember { mutableStateOf(Prefs.getBypassMinutes(context)) }
    var quoteType by remember { mutableStateOf(Prefs.getQuoteType(context)) }
    var showCustomCountdown by remember { mutableStateOf(false) }
    var showCustomBypass by remember { mutableStateOf(false) }

    var accessibilityOn by remember { mutableStateOf(isAccessibilityEnabled(context)) }
    var usageOn by remember { mutableStateOf(UsageTracker.hasUsageAccess(context)) }
    var batteryIgnored by remember { mutableStateOf(isIgnoringBattery(context)) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                accessibilityOn = isAccessibilityEnabled(context)
                usageOn = UsageTracker.hasUsageAccess(context)
                batteryIgnored = isIgnoringBattery(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topPadding + 16.dp, bottom = bottomPadding + 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { (context as? Activity)?.finish() }
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Settings",
                        style = InhaleTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.app_name_full),
                        style = InhaleTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )
                }
            }

            // --- Theme Selection ---
            SectionLabel("APPEARANCE", colors)
            SettingsCard(
                title = "App Theme",
                subtitle = "Choose your preferred visual aesthetic.",
                colors = colors
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val themes = listOf(
                        Prefs.ThemeMode.SYSTEM to "System",
                        Prefs.ThemeMode.DARK to "Dark",
                        Prefs.ThemeMode.LIGHT to "Light",
                        Prefs.ThemeMode.AMOLED to "OLED",
                    )
                    themes.forEach { (mode, label) ->
                        ModernChip(
                            text = label,
                            selected = themeMode == mode,
                            colors = colors,
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Pause Duration ---
            SectionLabel("BEHAVIOR", colors)
            SettingsCard(
                title = "Pause Duration",
                subtitle = "Default breathing wait before \"Open anyway\" unlocks.",
                colors = colors
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(3, 5, 10).forEach { secs ->
                        ModernChip(
                            text = "${secs}s",
                            selected = countdown == secs,
                            colors = colors,
                            onClick = {
                                countdown = secs
                                Prefs.setCountdown(context, secs)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModernChip(
                        text = if (countdown !in listOf(3, 5, 10)) "${countdown}s" else "Custom",
                        selected = countdown !in listOf(3, 5, 10),
                        colors = colors,
                        onClick = { showCustomCountdown = true },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }

            // --- Bypass Duration ---
            SettingsCard(
                title = "Bypass Duration",
                subtitle = "How long an app stays unpaused after choosing \"Open anyway\".",
                colors = colors
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(1, 5, 10).forEach { mins ->
                        ModernChip(
                            text = "${mins}m",
                            selected = bypassMinutes == mins,
                            colors = colors,
                            onClick = {
                                bypassMinutes = mins
                                Prefs.setBypassMinutes(context, mins)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModernChip(
                        text = if (bypassMinutes !in listOf(1, 5, 10)) "${bypassMinutes}m" else "Custom",
                        selected = bypassMinutes !in listOf(1, 5, 10),
                        colors = colors,
                        onClick = { showCustomBypass = true },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }

            // --- Mindfulness Quotes ---
            SettingsCard(
                title = "Mindfulness Quotes",
                subtitle = "Choose which quotes to reflect on during the pause.",
                colors = colors
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quoteTypes = listOf(
                        Prefs.QuoteType.GENERAL to "General",
                        Prefs.QuoteType.ISLAMIC to "Islamic",
                        Prefs.QuoteType.BOTH to "Both",
                    )
                    quoteTypes.forEach { (type, label) ->
                        ModernChip(
                            text = label,
                            selected = quoteType == type,
                            colors = colors,
                            onClick = {
                                quoteType = type
                                Prefs.setQuoteType(context, type)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Permissions ---
            SectionLabel("PERMISSIONS & ACCESS", colors)

            PermissionCard(
                title = "Accessibility Access",
                description = "Detects when mindful apps are opened to show the breathing pause.",
                granted = accessibilityOn,
                colors = colors,
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            PermissionCard(
                title = "Usage Stats Access",
                description = "Calculates screen time spent in each app over the last 24 hours.",
                granted = usageOn,
                colors = colors,
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            PermissionCard(
                title = "Background Continuity",
                description = "Prevents Android from killing the Inhale service in the background.",
                granted = batteryIgnored,
                colors = colors,
                onAction = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )

            Spacer(Modifier.height(16.dp))
        }

        if (showCustomCountdown) {
            NumberDialog(
                title = "Custom Pause Duration",
                body = "Seconds to wait before \"Open anyway\" unlocks (minimum ${Prefs.MIN_COUNTDOWN}s).",
                initial = if (countdown !in listOf(3, 5, 10)) countdown.toString() else "",
                min = Prefs.MIN_COUNTDOWN,
                colors = colors,
                onDismiss = { showCustomCountdown = false },
                onSave = {
                    countdown = it
                    Prefs.setCountdown(context, it)
                    showCustomCountdown = false
                }
            )
        }

        if (showCustomBypass) {
            NumberDialog(
                title = "Custom Bypass Duration",
                body = "Minutes an app stays unpaused after opening (minimum ${Prefs.MIN_BYPASS_MINUTES}m).",
                initial = if (bypassMinutes !in listOf(1, 5, 10)) bypassMinutes.toString() else "",
                min = Prefs.MIN_BYPASS_MINUTES,
                colors = colors,
                onDismiss = { showCustomBypass = false },
                onSave = {
                    bypassMinutes = it
                    Prefs.setBypassMinutes(context, it)
                    showCustomBypass = false
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(title: String, colors: InhaleColors) {
    Text(
        title,
        style = InhaleTheme.typography.labelSmall,
        color = colors.textTertiary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String? = null,
    colors: InhaleColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = InhaleTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = InhaleTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ModernChip(
    text: String,
    selected: Boolean,
    colors: InhaleColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceSubtle,
        label = "chipBg"
    )
    val textCol by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.textSecondary,
        label = "chipText"
    )
    val borderStroke = if (selected) null else BorderStroke(1.dp, colors.borderSubtle)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = borderStroke,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = InhaleTheme.typography.labelMedium,
                color = textCol,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    colors: InhaleColors,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onAction)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (granted) colors.success else colors.warning)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = InhaleTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = InhaleTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = (if (granted) colors.success else colors.warning).copy(alpha = 0.12f)
            ) {
                Text(
                    if (granted) "Granted" else "Not Granted",
                    style = InhaleTheme.typography.labelSmall,
                    color = if (granted) colors.success else colors.warning,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NumberDialog(
    title: String,
    body: String,
    initial: String,
    min: Int,
    colors: InhaleColors,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val parsed = text.trim().toIntOrNull()
    val valid = parsed != null && parsed >= min

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                title,
                style = InhaleTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    body,
                    style = InhaleTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 3) text = it.filter(Char::isDigit) },
                    singleLine = true,
                    isError = text.isNotEmpty() && !valid,
                    supportingText = {
                        if (text.isNotEmpty() && !valid) {
                            Text("Must be at least $min", color = colors.warning)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (valid) onSave(parsed!!) },
                enabled = valid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Text("Save", style = InhaleTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = InhaleTheme.typography.labelMedium, color = colors.textSecondary)
            }
        }
    )
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
        as android.view.accessibility.AccessibilityManager
    return am.getEnabledAccessibilityServiceList(
        android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    ).any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

private fun isIgnoringBattery(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
