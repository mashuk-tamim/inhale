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

    val selectedFriction = remember {
        val type = Prefs.getFrictionType(context)
        if (type == Prefs.FrictionType.RANDOM) {
            listOf(
                Prefs.FrictionType.BREATHING,
                Prefs.FrictionType.MATH,
                Prefs.FrictionType.TYPING,
                Prefs.FrictionType.HOLD,
                Prefs.FrictionType.INTENT
            ).random()
        } else {
            type
        }
    }
    val isBreathing = selectedFriction == Prefs.FrictionType.BREATHING

    var frictionCompleted by remember { mutableStateOf(false) }

    // State for Intent friction
    var selectedIntent by remember { mutableStateOf<String?>(null) }
    var isIntentSubmitted by remember { mutableStateOf(false) }

    // State for Typing friction
    val typingTargetPhrase = remember { typingPhrases.random() }
    var typingText by remember { mutableStateOf("") }
    var isTypingSubmitted by remember { mutableStateOf(false) }
    var typingShowError by remember { mutableStateOf(false) }

    // Countdown fraction — breathing only
    var fraction by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(countdownSeconds) {
        if (!isBreathing) return@LaunchedEffect
        val start = System.nanoTime() - ((1f - fraction) * countdownSeconds * 1_000_000_000L).toLong()
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
    val unlocked = if (isBreathing) fraction <= 0f else frictionCompleted

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
                        top = topPadding + 48.dp,
                        bottom = bottomPadding + 24.dp
                    )
            ) {
                // Unified Layout: Reflection Cards at Top, Exercise Centered in Middle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    QuoteCard(quote, compact, colors)

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

                    Spacer(Modifier.weight(1f))

                    // Friction challenge centered with ample clearance
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (compact) 10.dp else 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedFriction) {
                            Prefs.FrictionType.BREATHING -> {
                                BreathingExercise(
                                    progress = fraction,
                                    remaining = remainingSecs,
                                    size = circleSize,
                                    colors = colors
                                )
                            }
                            Prefs.FrictionType.HOLD -> {
                                HoldFriction(
                                    holdSeconds = countdownSeconds,
                                    size = circleSize,
                                    colors = colors,
                                    onCompleted = { frictionCompleted = it }
                                )
                            }
                            Prefs.FrictionType.MATH -> {
                                MathFriction(
                                    colors = colors,
                                    onCompleted = { frictionCompleted = it }
                                )
                            }
                            Prefs.FrictionType.TYPING -> {
                                TypingFriction(
                                    targetPhrase = typingTargetPhrase,
                                    text = typingText,
                                    onTextChange = {
                                        typingText = it
                                        typingShowError = false
                                    },
                                    isSubmitted = isTypingSubmitted,
                                    showError = typingShowError,
                                    colors = colors
                                )
                            }
                            Prefs.FrictionType.INTENT -> {
                                IntentFriction(
                                    selectedOption = selectedIntent,
                                    onSelectOption = { selectedIntent = it },
                                    isSubmitted = isIntentSubmitted,
                                    colors = colors
                                )
                            }
                            else -> {
                                BreathingExercise(
                                    progress = fraction,
                                    remaining = remainingSecs,
                                    size = circleSize,
                                    colors = colors
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))

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

                    // Secondary Slot: Submit Button for Intent/Typing before submission,
                    // otherwise "Open anyway" button!
                    val isIntentMode = selectedFriction == Prefs.FrictionType.INTENT
                    val isTypingMode = selectedFriction == Prefs.FrictionType.TYPING

                    if (isIntentMode && !isIntentSubmitted) {
                        val canSubmit = selectedIntent != null
                        Button(
                            onClick = {
                                if (canSubmit) {
                                    isIntentSubmitted = true
                                    frictionCompleted = true
                                }
                            },
                            enabled = canSubmit,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                                disabledContainerColor = colors.surfaceSubtle,
                                disabledContentColor = colors.textTertiary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                "Submit",
                                style = InhaleTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (compact) 14.sp else 15.sp
                            )
                        }
                    } else if (isTypingMode && !isTypingSubmitted) {
                        val canSubmit = typingText.isNotBlank()
                        Button(
                            onClick = {
                                if (normalize(typingText) == normalize(typingTargetPhrase)) {
                                    isTypingSubmitted = true
                                    typingShowError = false
                                    frictionCompleted = true
                                } else {
                                    typingShowError = true
                                }
                            },
                            enabled = canSubmit,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                                disabledContainerColor = colors.surfaceSubtle,
                                disabledContentColor = colors.textTertiary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                "Submit",
                                style = InhaleTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (compact) 14.sp else 15.sp
                            )
                        }
                    } else {
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
                                if (unlocked) "Open anyway"
                                else if (isBreathing) "Open anyway · ${remainingSecs}s"
                                else "Open anyway",
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
}

@Composable
private fun QuoteCard(
    quote: Pair<String, String>,
    compact: Boolean,
    colors: InhaleColors
) {
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
    BreathingExercise(
        progress = progress,
        remaining = remaining,
        size = size,
        colors = colors
    )
}
