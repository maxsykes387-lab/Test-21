package com.maxsykes.pixelorganiser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxsykes.pixelorganiser.CategoryGroup
import com.maxsykes.pixelorganiser.OrganiserViewModel
import com.maxsykes.pixelorganiser.data.AppEntry
import com.maxsykes.pixelorganiser.data.Category
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** iPhone-style rounded square used for every app icon. */
private val IconShape = RoundedCornerShape(percent = 26)

/** Text that stays readable over any wallpaper. */
private val WallpaperShadow = Shadow(color = Color.Black.copy(alpha = 0.45f), blurRadius = 8f)

/**
 * The home screen: date at the top, one row per category floating over the real wallpaper,
 * then a dock and search pill at the bottom like the stock Pixel launcher.
 * Each row scrolls sideways if it has more apps than fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganiserScreen(viewModel: OrganiserViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetApp by remember { mutableStateOf<AppEntry?>(null) }
    val listState = rememberLazyListState()

    // On a home screen, Back should never leave. It just clears the search.
    BackHandler(enabled = true) {
        if (state.query.isNotEmpty()) viewModel.setQuery("")
    }

    val allApps = remember(state.allGroups) { state.allGroups.flatMap { it.apps } }
    val dock = remember(allApps) { pickDock(allApps) }

    Column(
        Modifier
            .fillMaxSize()
            // A soft scrim so rows stay legible on bright wallpapers, while the wallpaper still shows.
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.08f),
                    0.5f to Color.Black.copy(alpha = 0.14f),
                    1f to Color.Black.copy(alpha = 0.30f),
                ),
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { AtAGlance() }

            when {
                state.loading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                state.groups.isEmpty() -> item {
                    Text(
                        "No apps match \"${state.query}\"",
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, shadow = WallpaperShadow),
                    )
                }
                else -> items(state.groups, key = { it.category.name }) { group ->
                    CategoryRow(
                        group = group,
                        onLaunch = viewModel.repository::launch,
                        onLongPress = { sheetApp = it },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        // Dock: a frosted tray of favourite apps (iPhone style), then the Pixel search pill.
        if (dock.isNotEmpty() && state.query.isEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(32.dp))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                dock.forEach { app ->
                    DockIcon(app, viewModel.repository::launch, { sheetApp = it })
                }
            }
        }
        SearchPill(state.query, viewModel::setQuery)
        Spacer(Modifier.height(8.dp))
    }

    sheetApp?.let { app ->
        val current = state.allGroups.firstOrNull { g -> g.apps.any { it.packageName == app.packageName } }?.category
        MoveSheet(
            app = app,
            current = current ?: app.autoCategory,
            onDismiss = { sheetApp = null },
            onMove = { viewModel.move(app, it); sheetApp = null },
            onAppInfo = { viewModel.repository.openAppInfo(app); sheetApp = null },
        )
    }
}

@Composable
private fun AtAGlance() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000L * (60 - now.second))
        }
    }
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val white = TextStyle(color = Color.White, shadow = WallpaperShadow)
    Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 18.dp)) {
        Text(
            now.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
            style = white.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal),
        )
        Text(greeting, style = white.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
    }
}

/** Phone, Messages, Wallet, Chrome and Camera if installed. */
private val DockPackages = listOf(
    "com.google.android.dialer",
    "com.google.android.apps.messaging",
    "com.google.android.apps.walletnfcrel",
    "com.android.chrome",
    "com.google.android.GoogleCamera",
)

private fun pickDock(apps: List<AppEntry>): List<AppEntry> =
    DockPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockIcon(app: AppEntry, onLaunch: (AppEntry) -> Unit, onLongPress: (AppEntry) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Image(
        app.icon,
        contentDescription = app.label,
        modifier = Modifier
            .size(58.dp)
            .clip(IconShape)
            .combinedClickable(
                onClick = { onLaunch(app) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress(app)
                },
            ),
    )
}

@Composable
private fun SearchPill(query: String, onQuery: (String) -> Unit) {
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).height(56.dp),
        placeholder = { Text("Search apps") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") }
            }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

/** A category's accent, softened for dark mode so it sits nicely next to Material You colours. */
@Composable
private fun Category.tint(): Color =
    if (isSystemInDarkTheme()) lerp(accent, Color.White, 0.45f) else accent

@Composable
private fun CategoryRow(
    group: CategoryGroup,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = group.category.tint()
    // Translucent Material You surface: the wallpaper shows through, but icons and labels stay crisp.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .border(1.dp, Color.White.copy(alpha = 0.22f), MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f),
    ) {
        Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(group.category.icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(group.category.label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                Text(
                    "${group.apps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(group.apps, key = { it.component.flattenToString() }) { app ->
                    AppTile(app, onLaunch, onLongPress, Modifier.width(78.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: AppEntry,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = { onLaunch(app) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress(app)
                },
                onLongClickLabel = "Move to another category",
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(app.icon, contentDescription = null, modifier = Modifier.size(54.dp).clip(IconShape))
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveSheet(
    app: AppEntry,
    current: Category,
    onDismiss: () -> Unit,
    onMove: (Category) -> Unit,
    onAppInfo: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(app.icon, contentDescription = null, modifier = Modifier.size(44.dp).clip(IconShape))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleLarge)
                Text("Move to a row", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAppInfo) { Icon(Icons.Rounded.Info, contentDescription = "App info") }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(Category.entries) { cat ->
                val isCurrent = cat == current
                val supporting: (@Composable () -> Unit)? =
                    if (cat == app.autoCategory) { { Text("Suggested") } } else null
                val trailing: (@Composable () -> Unit)? =
                    if (isCurrent) {
                        { Icon(Icons.Rounded.Check, contentDescription = "Current", tint = MaterialTheme.colorScheme.primary) }
                    } else null
                ListItem(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clip(CircleShape)
                        .clickable { onMove(cat) },
                    headlineContent = { Text(cat.label) },
                    supportingContent = supporting,
                    leadingContent = { Icon(cat.icon, contentDescription = null, tint = cat.tint()) },
                    trailingContent = trailing,
                    colors = ListItemDefaults.colors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    ),
                )
            }
        }
    }
}
