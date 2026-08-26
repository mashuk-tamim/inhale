package com.mashuktamim.inhale

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { InhaleApp() }
    }
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: android.graphics.Bitmap,
    val category: Int = android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED,
)

private fun categoryLabel(category: Int): String = when (category) {
    android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Games"
    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
    android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Video"
    android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "Music & Audio"
    android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Photos"
    android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
    android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "Maps & Navigation"
    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
    android.content.pm.ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Accessibility"
    else -> "Other"
}

private val CATEGORY_ORDER = listOf(
    "Social", "Games", "Video", "Music & Audio", "Photos",
    "Productivity", "News", "Maps & Navigation", "Other", "Accessibility",
)

@Composable
fun InhaleApp() {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(Prefs.getThemeMode(context)) }
    var targets by remember { mutableStateOf(Prefs.getTargets(context)) }
    val serviceEnabled = remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var usageAccess by remember { mutableStateOf(UsageTracker.hasUsageAccess(context)) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var statsTick by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                themeMode = Prefs.getThemeMode(context)
                serviceEnabled.value = isAccessibilityServiceEnabled(context)
                usageAccess = UsageTracker.hasUsageAccess(context)
                targets = Prefs.getTargets(context)
                statsTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(statsTick) {
        if (usageAccess) {
            usage = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                UsageTracker.getUsageLast24h(context)
            }
        }
    }

    val apps = remember { loadApps(context) }
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    val categorized = remember(filteredApps) {
        filteredApps.groupBy { categoryLabel(it.category) }
            .toSortedMap(compareBy({ CATEGORY_ORDER.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it }))
    }
    var expandedCategories by remember { mutableStateOf<Set<String>>(categorized.keys) }
    var isPausedExpanded by remember { mutableStateOf(false) }

    // Auto-expand categories during search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            expandedCategories = categorized.keys
        }
    }

    InhaleAppTheme(themeMode = themeMode) {
        val colors = InhaleTheme.colors
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = topPadding + 16.dp,
                    bottom = bottomPadding + 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Inhale",
                                    style = InhaleTheme.typography.displayMedium,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Breathe before you open",
                                style = InhaleTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }

                        // Settings Button
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    context.startActivity(
                                        Intent(context, SettingsActivity::class.java)
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Settings",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = InhaleTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                                cursorBrush = SolidColor(colors.primary),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Search apps...",
                                                style = InhaleTheme.typography.bodyMedium,
                                                color = colors.textTertiary
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = colors.textTertiary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }
                }

                // Accessibility Banner
                if (!serviceEnabled.value) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.warning)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Accessibility Access Required",
                                        style = InhaleTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Enable accessibility access so Inhale can detect when you open mindful apps and show the breathing pause.",
                                    style = InhaleTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Open Settings", style = InhaleTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Usage Access Banner
                if (!usageAccess) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "App Screen Time Access",
                                    style = InhaleTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Grant usage access to calculate real-time screen time spent in each app over the last 24h.",
                                    style = InhaleTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Grant Access", style = InhaleTheme.typography.labelMedium, color = colors.primary)
                                }
                            }
                        }
                    }
                }

                // Mindful Apps Containerized Card
                if (searchQuery.isEmpty() && targets.isNotEmpty()) {
                    val pausedApps = apps.filter { it.packageName in targets }
                    item(key = "mindful_apps_container") {
                        MindfulAppsContainer(
                            apps = pausedApps,
                            expanded = isPausedExpanded,
                            colors = colors,
                            onToggle = { isPausedExpanded = !isPausedExpanded },
                            onOpenApp = { app ->
                                context.startActivity(
                                    Intent(context, AppDetailActivity::class.java)
                                        .putExtra(AppDetailActivity.EXTRA_PACKAGE, app.packageName)
                                )
                            }
                        )
                    }
                }

                // Section Label
                item {
                    SectionHeader(
                        title = if (searchQuery.isEmpty()) "ALL APPS" else "SEARCH RESULTS",
                        count = "${filteredApps.size}",
                        colors = colors
                    )
                }

                // All Apps Category Containerized Cards
                categorized.forEach { (category, appsInCategory) ->
                    val expanded = category in expandedCategories
                    item(key = "cat_$category") {
                        CategoryContainer(
                            category = category,
                            apps = appsInCategory,
                            expanded = expanded,
                            targets = targets,
                            usage = if (usageAccess) usage else null,
                            colors = colors,
                            onToggleExpanded = {
                                expandedCategories =
                                    if (expanded) expandedCategories - category
                                    else expandedCategories + category
                            },
                            onToggleApp = { app ->
                                Prefs.toggleTarget(context, app.packageName)
                                targets = Prefs.getTargets(context)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: String, colors: InhaleColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    ) {
        Text(
            title,
            style = InhaleTheme.typography.labelSmall,
            color = colors.textTertiary
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = colors.surfaceSubtle,
            border = BorderStroke(1.dp, colors.borderSubtle)
        ) {
            Text(
                count,
                style = InhaleTheme.typography.labelSmall,
                color = colors.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Enclosed Mindful Apps Card with Header and contained inner children.
 */
@Composable
private fun MindfulAppsContainer(
    apps: List<AppInfo>,
    expanded: Boolean,
    colors: InhaleColors,
    onToggle: () -> Unit,
    onOpenApp: (AppInfo) -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "pausedChevron"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, if (expanded) colors.primary.copy(alpha = 0.35f) else colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Spa,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Mindful Apps",
                        style = InhaleTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        if (expanded) "Tap to collapse" else "Tap to view & customize pauses",
                        style = InhaleTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )
                }
                // Single numeric badge (e.g. 1, 4)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${apps.size}",
                        style = InhaleTheme.typography.labelSmall,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }

            // Expanded Children inside the container
            if (expanded) {
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                apps.forEachIndexed { index, app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenApp(app) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = InhaleTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Configure pause duration",
                                style = InhaleTheme.typography.bodySmall,
                                color = colors.textTertiary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (index < apps.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp),
                            color = colors.borderSubtle.copy(alpha = 0.5f),
                            thickness = 0.8.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enclosed Category Card with Header and contained inner app items.
 */
@Composable
private fun CategoryContainer(
    category: String,
    apps: List<AppInfo>,
    expanded: Boolean,
    targets: Set<String>,
    usage: Map<String, Long>?,
    colors: InhaleColors,
    onToggleExpanded: () -> Unit,
    onToggleApp: (AppInfo) -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "catChevron"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Category Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    category,
                    style = InhaleTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                // Single numeric badge (e.g. 10)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceSubtle,
                    border = BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Text(
                        "${apps.size}",
                        style = InhaleTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }

            // Expanded Apps list inside the category card
            if (expanded) {
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                apps.forEachIndexed { index, app ->
                    val selected = app.packageName in targets
                    val usageMs = usage?.get(app.packageName)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleApp(app) }
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = InhaleTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (usageMs != null && usageMs > 0) {
                                Text(
                                    "${formatDuration(usageMs)} today",
                                    style = InhaleTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        MinimalToggle(
                            checked = selected,
                            colors = colors,
                            onCheckedChange = { onToggleApp(app) }
                        )
                    }
                    if (index < apps.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 66.dp),
                            color = colors.borderSubtle.copy(alpha = 0.5f),
                            thickness = 0.8.dp
                        )
                    }
                }
            }
        }
    }
}

internal fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60_000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
private fun MinimalToggle(
    checked: Boolean,
    colors: InhaleColors,
    onCheckedChange: (Boolean) -> Unit
) {
    val toggleBg by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.surfaceSubtle,
        label = "toggleBg"
    )
    val borderStroke = if (checked) null else BorderStroke(1.dp, colors.border)

    Surface(
        shape = CircleShape,
        color = toggleBg,
        border = borderStroke,
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        if (checked) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

private fun loadApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcherIntent, 0)
        .mapNotNull { info ->
            try {
                AppInfo(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm).toBitmap(72, 72),
                    category = info.activityInfo.applicationInfo.category,
                )
            } catch (e: Exception) {
                null
            }
        }
        .filter { it.packageName != context.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}
