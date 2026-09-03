package com.mashuktamim.inhale

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Animated checkmark with spring bounce and progressive stroke drawing.
 */
@Composable
fun AnimatedCheckmark(
    size: Dp = 48.dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0f) }
    val pathProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(Unit) {
        delay(80)
        pathProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 320, easing = EaseOutCubic)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale.value)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = (size.toPx() * 0.085f).coerceAtLeast(3.dp.toPx())

            // Ambient background halo
            drawCircle(
                color = color.copy(alpha = 0.14f),
                radius = size.toPx() / 2f
            )
            drawCircle(
                color = color.copy(alpha = 0.40f),
                radius = size.toPx() / 2f,
                style = Stroke(width = 1.5.dp.toPx())
            )

            val w = size.toPx()
            val h = size.toPx()
            val p1 = Offset(w * 0.28f, h * 0.52f)
            val p2 = Offset(w * 0.44f, h * 0.68f)
            val p3 = Offset(w * 0.72f, h * 0.36f)

            val progress = pathProgress.value
            if (progress > 0f) {
                if (progress <= 0.4f) {
                    val segProgress = progress / 0.4f
                    val current = Offset(
                        p1.x + (p2.x - p1.x) * segProgress,
                        p1.y + (p2.y - p1.y) * segProgress
                    )
                    drawLine(
                        color = color,
                        start = p1,
                        end = current,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                } else {
                    drawLine(
                        color = color,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    val segProgress = (progress - 0.4f) / 0.6f
                    val current = Offset(
                        p2.x + (p3.x - p2.x) * segProgress,
                        p2.y + (p3.y - p2.y) * segProgress
                    )
                    drawLine(
                        color = color,
                        start = p2,
                        end = current,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Shared circular progress UI for Breathing and Hold-to-Unlock.
 * Displays an outer arc, 3 ambient glowing spheres, center countdown/checkmark,
 * and the label text (Inhale/Hold/Exhale or Hold to unlock/Keep holding) OUTSIDE the ring below.
 */
@Composable
fun CircularExercise(
    progress: Float,
    remaining: Int,
    cueLabel: String,
    size: Dp,
    colors: InhaleColors,
    innerScale: Float = 1f,
    isCompleted: Boolean = false,
    onHoldChange: ((Boolean) -> Unit)? = null
) {
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
            modifier = Modifier
                .size(size)
                .then(
                    if (onHoldChange != null && !isCompleted) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onHoldChange(true)
                                    tryAwaitRelease()
                                    onHoldChange(false)
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            // Outer Track & Animated Progress Arc
            Canvas(Modifier.size(size)) {
                val strokeWidthPx = strokeWidthDp.toPx()
                val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                drawArc(
                    color = colors.borderSubtle.copy(alpha = 0.55f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    brush = ringBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = stroke
                )
            }

            val innerDiameter = size - strokeWidthDp

            // Ambient pulsating sphere 1
            Box(
                Modifier
                    .size(innerDiameter)
                    .scale(innerScale)
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

            // Ambient pulsating sphere 2
            Box(
                Modifier
                    .size(innerDiameter * 0.74f)
                    .scale(innerScale)
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

            // Ambient pulsating sphere 3
            Box(
                Modifier
                    .size(innerDiameter * 0.48f)
                    .scale(innerScale)
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

            // Center Content: Countdown number or AnimatedCheckmark
            if (isCompleted) {
                AnimatedCheckmark(
                    size = (size.value * 0.32f).coerceIn(44f, 64f).dp,
                    color = colors.success
                )
            } else {
                Text(
                    "$remaining",
                    style = InhaleTheme.typography.displayMedium,
                    fontSize = (size.value * 0.25f).sp,
                    color = colors.textPrimary
                )
            }
        }

        // Cue text strictly outside the circle below
        Text(
            cueLabel,
            style = InhaleTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isCompleted) colors.success else colors.primary,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/**
 * Breathing exercise wrapper using CircularExercise.
 */
@Composable
fun BreathingExercise(
    progress: Float,
    remaining: Int,
    size: Dp,
    colors: InhaleColors
) {
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

    CircularExercise(
        progress = progress,
        remaining = remaining,
        cueLabel = breathLabel,
        size = size,
        colors = colors,
        innerScale = breath.value
    )
}

/**
 * Hold to unlock exercise reusing CircularExercise.
 * Moves "Hold to unlock" and "Keep holding" OUT of the ring like "Inhale" and "Exhale".
 */
@Composable
fun HoldFriction(
    holdSeconds: Int,
    size: Dp,
    colors: InhaleColors,
    onCompleted: (Boolean) -> Unit
) {
    var holding by remember { mutableStateOf(false) }
    val fill = remember { Animatable(0f) }
    val done = fill.value >= 1f

    LaunchedEffect(holding) {
        if (done) return@LaunchedEffect
        if (holding) {
            fill.animateTo(1f, tween(holdSeconds * 1000, easing = LinearEasing))
            if (fill.value >= 1f) onCompleted(true)
        } else {
            fill.animateTo(0f, tween(250))
        }
    }

    val remaining = kotlin.math.ceil((1f - fill.value) * holdSeconds).toInt().coerceAtLeast(0)
    val label = when {
        done -> "Unlocked"
        holding -> "Keep holding"
        else -> "Hold to unlock"
    }

    CircularExercise(
        progress = if (done) 1f else fill.value,
        remaining = remaining,
        cueLabel = label,
        size = size,
        colors = colors,
        innerScale = 0.36f + 0.64f * fill.value,
        isCompleted = done,
        onHoldChange = { holding = it }
    )
}

/** Wide card host for interactive frictions (math/typing/intent). */
@Composable
fun FrictionCard(
    colors: InhaleColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            content = content
        )
    }
}

@Composable
fun MathFriction(
    colors: InhaleColors,
    onCompleted: (Boolean) -> Unit
) {
    val a = remember { (10..50).random() }
    val b = remember { (10..50).random() }
    var answer by remember { mutableStateOf("") }
    val correct = (a + b).toString() == answer.trim()

    LaunchedEffect(correct) {
        if (correct) onCompleted(true)
    }

    FrictionCard(colors) {
        if (correct) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                AnimatedCheckmark(size = 40.dp, color = colors.success)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Solved! ($a + $b = ${a + b})",
                    style = InhaleTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
        } else {
            Text(
                "SOLVE TO CONTINUE",
                style = InhaleTheme.typography.labelSmall,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = colors.textTertiary
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$a + $b =",
                    style = InhaleTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.width(14.dp))
                BasicTextField(
                    value = answer,
                    onValueChange = { if (it.length <= 4) answer = it.filter(Char::isDigit) },
                    textStyle = TextStyle(
                        color = colors.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier
                        .width(110.dp)
                        .background(colors.surfaceSubtle, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

val typingPhrases = listOf(
    "I am opening this app with intention, not out of habit.",
    "The scroll can wait; my attention belongs to me right now.",
    "I will use this app for what I came for, then leave.",
    "A moment of pause is worth more than an hour of scrolling."
)

@Composable
fun TypingFriction(
    targetPhrase: String,
    text: String,
    onTextChange: (String) -> Unit,
    isSubmitted: Boolean,
    showError: Boolean,
    colors: InhaleColors
) {
    FrictionCard(colors) {
        if (isSubmitted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                AnimatedCheckmark(size = 40.dp, color = colors.success)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Sentence matched!",
                    style = InhaleTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
        } else {
            Text(
                "TYPE TO CONTINUE",
                style = InhaleTheme.typography.labelSmall,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = colors.textTertiary
            )
            Spacer(Modifier.height(10.dp))

            // Compare sentences using color to show the mismatch area
            if (showError) {
                val targetWords = targetPhrase.trim().split(Regex("\\s+"))
                val userWords = text.trim().split(Regex("\\s+"))

                val annotatedTarget = buildAnnotatedString {
                    targetWords.forEachIndexed { index, word ->
                        val userWord = userWords.getOrNull(index)
                        when {
                            userWord == null -> {
                                withStyle(SpanStyle(color = colors.textTertiary)) {
                                    append(word)
                                }
                            }
                            userWord.equals(word, ignoreCase = true) -> {
                                withStyle(SpanStyle(color = colors.success, fontWeight = FontWeight.Medium)) {
                                    append(word)
                                }
                            }
                            else -> {
                                withStyle(
                                    SpanStyle(
                                        color = colors.warning,
                                        fontWeight = FontWeight.Bold,
                                        background = colors.warning.copy(alpha = 0.22f)
                                    )
                                ) {
                                    append(word)
                                }
                            }
                        }
                        append(" ")
                    }
                }

                Text(
                    text = annotatedTarget,
                    style = InhaleTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sentence does not match. Check highlighted words.",
                    style = InhaleTheme.typography.labelSmall,
                    color = colors.warning,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    targetPhrase,
                    style = InhaleTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    color = colors.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSubtle, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (showError) colors.warning else colors.borderSubtle,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

fun normalize(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")

val intentOptions = listOf(
    "Boredom / Killing time",
    "Habit / Muscle memory",
    "Quick task or message",
    "Important / Need to use"
)

@Composable
fun IntentFriction(
    selectedOption: String?,
    onSelectOption: (String) -> Unit,
    isSubmitted: Boolean,
    colors: InhaleColors
) {
    FrictionCard(colors) {
        if (isSubmitted) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedCheckmark(size = 44.dp, color = colors.success)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Intent recorded",
                    style = InhaleTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "“${selectedOption ?: ""}”",
                    style = InhaleTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                "WHY ARE YOU OPENING THIS?",
                style = InhaleTheme.typography.labelSmall,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = colors.textTertiary
            )
            Spacer(Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                intentOptions.forEach { option ->
                    val isSelected = selectedOption == option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceSubtle,
                        border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectOption(option) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.borderSubtle
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = option,
                                style = InhaleTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
