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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
        val themeMode = Prefs.getThemeMode(this)

        setContent {
            InhaleAppTheme(themeMode = themeMode) {
                InhaleScreen(
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
                                Intent(this, AppDetailActivity::class.java).apply {
                                    putExtra(AppDetailActivity.EXTRA_PACKAGE, targetPackage)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
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

/** Mindfulness quotes rotated on each pause screen. */
private val QUOTES = listOf(
    "You are the sky. Everything else is just the weather." to "Pema Chödrön",
    "The present moment is the only moment available to us." to "Thích Nhất Hạnh",
    "Wherever you are, be all there." to "Jim Elliot",
    "Nothing is worth more than this day." to "Goethe",
    "Simplicity is the ultimate sophistication." to "Leonardo da Vinci",
    "Almost everything will work again if you unplug it for a few minutes — including you." to "Anne Lamott",
    "Nature does not hurry, yet everything is accomplished." to "Lao Tzu",
    "Breathe. You are exactly where you need to be." to "Unknown",
    "Attention is the rarest and purest form of generosity." to "Simone Weil",
    "The best way to capture moments is to pay attention." to "Jon Kabat-Zinn",
)

@Composable
fun InhaleScreen(
    countdownSeconds: Int,
    onOpenAnyway: () -> Unit,
    onMindfulChoice: () -> Unit,
) {
    val colors = InhaleTheme.colors
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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

    val quote = remember { QUOTES.random() }

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
            val circleSize = (maxHeight.value * 0.30f).coerceIn(180f, 250f).dp
            val compact = maxHeight < 640.dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = topPadding + 42.dp,
                        bottom = bottomPadding + 28.dp
                    )
            ) {
                // Header Cue with generous top margin away from camera
                Text(
                    "Take a breath",
                    style = InhaleTheme.typography.headlineMedium,
                    fontSize = if (compact) 24.sp else 28.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Middle area: Breathing Circle + Quote
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    BreathingCircle(
                        progress = fraction,
                        remaining = remainingSecs,
                        size = circleSize,
                        colors = colors
                    )

                    Spacer(Modifier.height(if (compact) 20.dp else 36.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                "“${quote.first}”",
                                style = InhaleTheme.typography.bodyLarge,
                                fontSize = if (compact) 14.sp else 16.sp,
                                lineHeight = if (compact) 22.sp else 25.sp,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "— ${quote.second}",
                                style = InhaleTheme.typography.labelSmall,
                                color = colors.textTertiary
                            )
                        }
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
                            "Stay Mindful · See Insights",
                            style = InhaleTheme.typography.titleMedium,
                            fontSize = if (compact) 15.sp else 16.sp
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
                            style = InhaleTheme.typography.bodyMedium,
                            fontSize = if (compact) 14.sp else 15.sp
                        )
                    }
                }
            }
        }
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

    val strokeWidthDp = 12.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            color = colors.primary,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
