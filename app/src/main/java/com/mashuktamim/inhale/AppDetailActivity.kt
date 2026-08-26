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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var weekly by remember { mutableStateOf<List<Long>>(emptyList()) }
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
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val last24 = UsageTracker.getUsageLast24h(context)[pkg]
                val perDay = UsageTracker.getUsagePerDay(context, pkg, 7)
                usage to perDay
            }.let { (u, w) ->
                usage = u
                weekly = w
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
                        .clickable {
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                            )
                            (context as? Activity)?.finish()
                        }
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

            // Stats Grid — colorful, one accent per metric
            val s = Prefs.getStats(context, pkg)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = "SCREEN TIME · 24H",
                        value = usage?.let { formatDuration(it) } ?: "—",
                        accent = colors.primary,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "TIMES OPENED",
                        value = "${s.opens}",
                        accent = colors.warning,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = "MINDFUL EXITS",
                        value = "${s.blocked}",
                        accent = colors.success,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "TOTAL OPEN TIME",
                        value = formatDuration(s.openTimeMs),
                        accent = colors.primary,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 7-day usage chart + mindful score
            if (usageAccess) {
                WeeklyUsageChart(weekly = weekly, colors = colors)
                MindfulScoreCard(opens = s.opens, blocked = s.blocked, colors = colors)
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
private fun StatCard(
    label: String,
    value: String,
    accent: Color,
    colors: InhaleColors,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.85f))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = InhaleTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = InhaleTheme.typography.titleLarge,
                color = colors.textPrimary,
                maxLines = 1
            )
        }
    }
}

/** Rounded gradient bar chart of screen time over the last 7 days. */
@Composable
private fun WeeklyUsageChart(weekly: List<Long>, colors: InhaleColors) {
    // Animate bars in when data arrives
    val progress by animateFloatAsState(
        targetValue = if (weekly.isEmpty()) 0f else 1f,
        animationSpec = tween(700),
        label = "chartProgress"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "This Week",
                style = InhaleTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Screen time over the last 7 days",
                style = InhaleTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(14.dp))
            if (weekly.isEmpty()) {
                Text(
                    "Loading…",
                    style = InhaleTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                val labels = remember {
                    val dow = java.text.SimpleDateFormat("EE", java.util.Locale.getDefault())
                    val cal = java.util.Calendar.getInstance()
                    (6 downTo 0).map { back ->
                        cal.timeInMillis = System.currentTimeMillis() - back * 24L * 60 * 60 * 1000
                        dow.format(cal.time).take(2)
                    }
                }
                Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                    val maxMs = (weekly.maxOrNull() ?: 0L).coerceAtLeast(30 * 60 * 1000L)
                    val gap = 10.dp.toPx()
                    val barW = (size.width - gap * 6) / 7f
                    labels.forEachIndexed { i, day ->
                        val frac = (weekly[i].toFloat() / maxMs).coerceIn(0f, 1f) * progress
                        val h = (size.height - 22.dp.toPx()) * frac
                        val left = i * (barW + gap)
                        if (h > 1f) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        colors.primary.copy(alpha = 0.95f),
                                        colors.primary.copy(alpha = 0.35f)
                                    ),
                                    startY = size.height - 22.dp.toPx() - h,
                                    endY = size.height - 22.dp.toPx()
                                ),
                                topLeft = Offset(left, size.height - 22.dp.toPx() - h),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(barW / 3f, barW / 3f)
                            )
                        } else {
                            drawRoundRect(
                                color = colors.borderSubtle,
                                topLeft = Offset(left, size.height - 22.dp.toPx() - 4.dp.toPx()),
                                size = Size(barW, 4.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            day,
                            left + barW / 2f,
                            size.height,
                            android.graphics.Paint().apply {
                                textSize = 11.dp.toPx()
                                color = android.graphics.Color.argb(
                                    (colors.textTertiary.alpha * 255).toInt(),
                                    (colors.textTertiary.red * 255).toInt(),
                                    (colors.textTertiary.green * 255).toInt(),
                                    (colors.textTertiary.blue * 255).toInt()
                                )
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Share of pause screens that ended in a mindful exit, shown as a progress ring. */
@Composable
private fun MindfulScoreCard(opens: Int, blocked: Int, colors: InhaleColors) {
    val total = opens + blocked
    val score = if (total == 0) 0f else blocked.toFloat() / total
    val progress by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(700),
        label = "scoreProgress"
    )
    val scoreColor = when {
        score >= 0.5f -> colors.success
        score >= 0.25f -> colors.warning
        else -> colors.primary
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(76.dp)) {
                    val stroke = 8.dp.toPx()
                    drawArc(
                        color = colors.borderSubtle,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    "${(score * 100).toInt()}%",
                    style = InhaleTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Mindful Score",
                    style = InhaleTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (total == 0) "No pauses yet — your score grows as you choose to stay mindful."
                    else "You walked away $blocked of $total times this app tried to pull you back.",
                    style = InhaleTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
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
