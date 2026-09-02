package com.mashuktamim.inhale

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp

/**
 * Full-screen pause shown over a target app. Offers a breathing exercise and
 * a continuously animated countdown; "Open anyway" unlocks once it finishes.
 */
class InhaleActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        val targetLabel = targetPackage?.let {
            try {
                packageManager.getApplicationInfo(it, 0).loadLabel(packageManager).toString()
            } catch (e: Exception) {
                null
            }
        }
        val themeMode = Prefs.getThemeMode(this)

        setContent {
            InhaleAppTheme(themeMode = themeMode) {
                InhaleScreen(
                    targetPackage = targetPackage,
                    targetLabel = targetLabel,
                    countdownSeconds = targetPackage?.let { Prefs.getEffectiveCountdown(this, it) }
                        ?: Prefs.getCountdown(this),
                    onOpenAnyway = {
                        if (targetPackage != null) {
                            InhaleDetectionService.allowPackage(
                                targetPackage,
                                Prefs.getEffectiveBypassMinutes(this, targetPackage) * 60_000L
                            )
                            Prefs.recordOpen(this, targetPackage)
                            packageManager.getLaunchIntentForPackage(targetPackage)?.let {
                                startActivity(it)
                            }
                        }
                        finish()
                    },
                    onMindfulChoice = {
                        if (targetPackage != null) {
                            Prefs.recordBlocked(this, targetPackage)
                            startActivity(
                                Intent(Intent.ACTION_MAIN)
                                    .addCategory(Intent.CATEGORY_HOME)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } else {
                            startActivity(
                                Intent(this, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                            )
                        }
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun InhaleScreen(
    targetPackage: String?,
    targetLabel: String?,
    countdownSeconds: Int,
    onOpenAnyway: () -> Unit,
    onMindfulChoice: () -> Unit,
) {
    val context = LocalContext.current
    val colors = InhaleTheme.colors
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Compact 24h insights for the paused app
    val stats = remember(targetPackage) {
        targetPackage?.let { Prefs.getStats(context, it) }
    }
    var usage24h by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(targetPackage) {
        if (targetPackage != null && UsageTracker.hasUsageAccess(context)) {
            usage24h = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                UsageTracker.getUsageLast24h(context)[targetPackage]
            }
        }
    }

    // Fraction remaining, animated continuously every frame.
    var fraction by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(countdownSeconds) {
        val start = System.nanoTime()
        val totalNs = countdownSeconds * 1_000_000_000L
        while (true) {
            val elapsed = System.nanoTime() - start
            fraction = (1f - elapsed.toFloat() / totalNs).coerceIn(0f, 1f)
            if (fraction <= 0f) break
            withFrameNanos { }
        }
        fraction = 0f
    }
    val remainingSecs = (fraction * countdownSeconds).toInt().coerceAtLeast(0)
    val unlocked = fraction <= 0f

    val quote = remember { Quotes.getRandom(Prefs.getQuoteType(context)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Soft ambient radial glow
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.accentGlow, Color.Transparent),
                        center = Offset(0.5f, 0.38f),
                        radius = 900f
                    )
                )
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val circleSize = (maxHeight.value * 0.22f).coerceIn(130f, 180f).dp
            val compact = maxHeight < 640.dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = topPadding + 44.dp,
                        bottom = bottomPadding + 24.dp
                    )
            ) {
                // Header Cue — well clear of the camera cutout
                Text(
                    "Take a breath",
                    style = InhaleTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 17.sp else 19.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = if (compact) 14.dp else 18.dp)
                )

                // Middle area: Breathing Circle + Quote + Insights.
                // Top-aligned so the circle sits right under the header;
                // scrollable so nothing clips on short screens.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    BreathingCircle(
                        progress = fraction,
                        remaining = remainingSecs,
                        size = circleSize,
                        colors = colors
                    )

                    Spacer(Modifier.height(if (compact) 14.dp else 18.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                "“${quote.first}”",
                                style = InhaleTheme.typography.bodyMedium,
                                fontSize = if (compact) 13.sp else 14.sp,
                                lineHeight = if (compact) 19.sp else 21.sp,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "— ${quote.second}",
                                style = InhaleTheme.typography.labelSmall,
                                color = colors.textTertiary
                            )
                        }
                    }

                    if (targetPackage != null) {
                        Spacer(Modifier.height(if (compact) 10.dp else 12.dp))
                        CompactInsights(
                            usage24h = usage24h,
                            opens = stats?.opens ?: 0,
                            blocked = stats?.blocked ?: 0,
                            compact = compact,
                            colors = colors
                        )
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary Highlighted Mindful Exit Button -> Opens App Detail
                    Button(
                        onClick = onMindfulChoice,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            targetLabel?.let { "I don't want to open $it" } ?: "I don't want to open",
                            style = InhaleTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (compact) 14.sp else 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Secondary De-emphasized Button -> Open anyway
                    OutlinedButton(
                        onClick = onOpenAnyway,
                        enabled = unlocked,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (unlocked) colors.surfaceSubtle else Color.Transparent,
                            contentColor = if (unlocked) colors.textSecondary else colors.textTertiary,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = colors.textTertiary.copy(alpha = 0.6f)
                        ),
                        border = if (unlocked) {
                            BorderStroke(1.dp, colors.borderSubtle)
                        } else {
                            BorderStroke(1.dp, colors.borderSubtle.copy(alpha = 0.25f))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            if (unlocked) "Open anyway" else "Open anyway · ${remainingSecs}s",
                            style = InhaleTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (compact) 14.sp else 15.sp
                        )
                    }
                }
            }
        }
    }
}

/** One-card "last 24h" glance for the paused app, same numbers as its detail page. */
@Composable
private fun CompactInsights(
    usage24h: Long?,
    opens: Int,
    blocked: Int,
    compact: Boolean,
    colors: InhaleColors
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                "LAST 24 HOURS",
                style = InhaleTheme.typography.labelMedium,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                InsightCell(
                    value = usage24h?.let { formatDuration(it) } ?: "—",
                    label = "SCREEN TIME",
                    accent = colors.primary,
                    colors = colors,
                    modifier = Modifier.weight(1.2f),
                    compact = compact
                )
                VerticalDivider(
                    modifier = Modifier.height(26.dp),
                    color = colors.borderSubtle.copy(alpha = 0.6f),
                    thickness = 1.dp
                )
                InsightCell(
                    value = "$opens",
                    label = "OPENED",
                    accent = colors.warning,
                    colors = colors,
                    modifier = Modifier.weight(0.7f),
                    compact = compact
                )
                VerticalDivider(
                    modifier = Modifier.height(26.dp),
                    color = colors.borderSubtle.copy(alpha = 0.6f),
                    thickness = 1.dp
                )
                InsightCell(
                    value = "$blocked",
                    label = "MINDFUL EXITS",
                    accent = colors.success,
                    colors = colors,
                    modifier = Modifier.weight(1.1f),
                    compact = compact
                )
            }
        }
    }
}

@Composable
private fun InsightCell(
    value: String,
    label: String,
    accent: Color,
    colors: InhaleColors,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = InhaleTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 17.sp else 18.sp,
            color = accent,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = InhaleTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = colors.textTertiary,
            maxLines = 1
        )
    }
}

@Composable
fun BreathingCircle(
    progress: Float,
    remaining: Int,
    size: Dp = 240.dp,
    colors: InhaleColors
) {
    // 4-2-4 breathing cycle: inhale 4s (expand), hold 2s (full), exhale 4s (contract)
    val breath = remember { Animatable(0.36f) }
    var breathLabel by remember { mutableStateOf("Inhale") }
    LaunchedEffect(Unit) {
        while (true) {
            breathLabel = "Inhale"
            breath.animateTo(1f, tween(4000, easing = EaseInOutSine))
            breathLabel = "Hold"
            delay(2000)
            breathLabel = "Exhale"
            breath.animateTo(0.36f, tween(4000, easing = EaseInOutSine))
        }
    }
    val scale = breath.value

    val ringBrush = Brush.sweepGradient(
        listOf(
            colors.primary,
            colors.accentGlow,
            colors.primary.copy(alpha = 0.8f),
            colors.primary
        )
    )

    val strokeWidthDp = 18.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = strokeWidthDp / 2)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
        // Outer Countdown Progress Arc with prominent 12dp width
        Canvas(Modifier.size(size)) {
            val strokeWidthPx = strokeWidthDp.toPx()
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            // Background Track
            drawArc(
                color = colors.borderSubtle.copy(alpha = 0.55f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            // Active Progress
            drawArc(
                brush = ringBrush,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke
            )
        }

        // Inner diameter available inside the 12dp outer ring
        val innerDiameter = size - strokeWidthDp

        // Ambient pulsating sphere 1: expands completely to the inner boundary of the 12dp ring
        Box(
            Modifier
                .size(innerDiameter)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to colors.primary.copy(alpha = 0.38f),
                            0.70f to colors.primary.copy(alpha = 0.28f),
                            0.94f to colors.primary.copy(alpha = 0.16f),
                            1.0f to colors.primary.copy(alpha = 0.04f)
                        )
                    )
                )
        )

        // Pulsating sphere 2: middle body wave
        Box(
            Modifier
                .size(innerDiameter * 0.74f)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to colors.primary.copy(alpha = 0.55f),
                            0.75f to colors.primary.copy(alpha = 0.25f),
                            1.0f to Color.Transparent
                        )
                    )
                )
        )

        // Pulsating sphere 3: core glow
        Box(
            Modifier
                .size(innerDiameter * 0.48f)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to colors.primary.copy(alpha = 0.70f),
                            1.0f to colors.primary.copy(alpha = 0.20f)
                        )
                    )
                )
        )

        // Center Countdown
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "$remaining",
                style = InhaleTheme.typography.displayMedium,
                fontSize = (size.value * 0.25f).sp,
                color = colors.textPrimary
            )
        }
        }

        // Breath cue below the circle
        Text(
            breathLabel,
            style = InhaleTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
