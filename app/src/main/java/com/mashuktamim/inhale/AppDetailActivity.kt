package com.mashuktamim.inhale

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * Per-app dashboard for a paused app: stats, last-24h screen time and
 * pause/bypass duration overrides for this app only.
 */
class AppDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "package_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        if (pkg == null) {
            finish()
            return
        }
        val themeMode = Prefs.getThemeMode(this)
        setContent {
            InhaleAppTheme(themeMode = themeMode) {
                AppDetailScreen(pkg)
            }
        }
    }
}

@Composable
private fun AppDetailScreen(pkg: String) {
    val context = LocalContext.current
    val colors = InhaleTheme.colors
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val app = remember(pkg) { loadApp(context, pkg) }
    var usage by remember { mutableStateOf<Long?>(null) }
    var usageAccess by remember { mutableStateOf(UsageTracker.hasUsageAccess(context)) }
    var appCountdown by remember { mutableStateOf(Prefs.getAppCountdown(context, pkg)) }
    var appBypass by remember { mutableStateOf(Prefs.getAppBypassMinutes(context, pkg)) }
    var showCustomCountdown by remember { mutableStateOf(false) }
    var showCustomBypass by remember { mutableStateOf(false) }
    val globalCountdown = remember { Prefs.getCountdown(context) }
    val globalBypass = remember { Prefs.getBypassMinutes(context) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                usageAccess = UsageTracker.hasUsageAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(usageAccess) {
        if (usageAccess) {
            usage = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                UsageTracker.getUsageLast24h(context)[pkg]
            }
        }
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
                modifier = Modifier.padding(bottom = 4.dp)
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
            }

            // App Header Card
            if (app != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = InhaleTheme.typography.titleLarge,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                pkg,
                                style = InhaleTheme.typography.bodySmall,
                                color = colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "PAUSED",
                                style = InhaleTheme.typography.labelSmall,
                                color = colors.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Stats Grid
            val s = Prefs.getStats(context, pkg)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("SCREEN TIME (24H)", usage?.let { formatDuration(it) } ?: "—", colors, Modifier.weight(1f))
                    StatCard("TIMES OPENED", "${s.opens}", colors, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("MINDFUL EXITS", "${s.blocked}", colors, Modifier.weight(1f))
                    StatCard("TOTAL OPEN TIME", formatDuration(s.openTimeMs), colors, Modifier.weight(1f))
                }
            }

            // Pause Duration Override
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Pause Duration Override",
                        style = InhaleTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Custom pause countdown for this app only.",
                        style = InhaleTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OverrideChip(
                            text = "Global (${globalCountdown}s)",
                            selected = appCountdown == null,
                            colors = colors,
                            onClick = {
                                Prefs.setAppCountdown(context, pkg, null)
                                appCountdown = null
                            },
                            modifier = Modifier.weight(1.4f)
                        )
                        listOf(3, 5, 10).forEach { secs ->
                            OverrideChip(
                                text = "${secs}s",
                                selected = appCountdown == secs,
                                colors = colors,
                                onClick = {
                                    Prefs.setAppCountdown(context, pkg, secs)
                                    appCountdown = secs
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (appCountdown != null && appCountdown !in listOf(3, 5, 10)) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Custom: ${appCountdown}s (Tap to modify)",
                            style = InhaleTheme.typography.labelMedium,
                            color = colors.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showCustomCountdown = true }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Bypass Duration Override
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Bypass Duration Override",
                        style = InhaleTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Unpaused duration after bypass for this app only.",
                        style = InhaleTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OverrideChip(
                            text = "Global (${globalBypass}m)",
                            selected = appBypass == null,
                            colors = colors,
                            onClick = {
                                Prefs.setAppBypassMinutes(context, pkg, null)
                                appBypass = null
                            },
                            modifier = Modifier.weight(1.4f)
                        )
                        listOf(1, 5, 10).forEach { mins ->
                            OverrideChip(
                                text = "${mins}m",
                                selected = appBypass == mins,
                                colors = colors,
                                onClick = {
                                    Prefs.setAppBypassMinutes(context, pkg, mins)
                                    appBypass = mins
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (appBypass != null && appBypass !in listOf(1, 5, 10)) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Custom: ${appBypass}m (Tap to modify)",
                            style = InhaleTheme.typography.labelMedium,
                            color = colors.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showCustomBypass = true }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Actions
            OutlinedButton(
                onClick = {
                    Prefs.toggleTarget(context, pkg)
                    (context as? Activity)?.finish()
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    "Remove from Paused Apps",
                    style = InhaleTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        if (showCustomCountdown) {
            CustomNumberDialog(
                title = "Custom Pause Duration",
                body = "Seconds to wait before \"Open anyway\" unlocks (minimum ${Prefs.MIN_COUNTDOWN}s).",
                initial = appCountdown?.toString() ?: "",
                min = Prefs.MIN_COUNTDOWN,
                colors = colors,
                onDismiss = { showCustomCountdown = false },
                onSave = {
                    Prefs.setAppCountdown(context, pkg, it)
                    appCountdown = it
                    showCustomCountdown = false
                }
            )
        }
        if (showCustomBypass) {
            CustomNumberDialog(
                title = "Custom Bypass Duration",
                body = "Minutes the app stays unpaused after \"Open anyway\" (minimum ${Prefs.MIN_BYPASS_MINUTES}m).",
                initial = appBypass?.toString() ?: "",
                min = Prefs.MIN_BYPASS_MINUTES,
                colors = colors,
                onDismiss = { showCustomBypass = false },
                onSave = {
                    Prefs.setAppBypassMinutes(context, pkg, it)
                    appBypass = it
                    showCustomBypass = false
                }
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, colors: InhaleColors, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                value,
                style = InhaleTheme.typography.titleLarge,
                color = colors.textPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = InhaleTheme.typography.labelSmall,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun OverrideChip(
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
            Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
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
private fun CustomNumberDialog(
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

private fun loadApp(context: Context, pkg: String): AppInfo? {
    return try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(pkg, 0)
        AppInfo(
            packageName = pkg,
            label = info.loadLabel(pm).toString(),
            icon = info.loadIcon(pm).toBitmap(72, 72),
        )
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: Exception) {
        null
    }
}
