package com.maxsykes.pixelorganiser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxsykes.pixelorganiser.CategoryGroup
import com.maxsykes.pixelorganiser.OrganiserViewModel
import com.maxsykes.pixelorganiser.data.AppEntry
import com.maxsykes.pixelorganiser.data.Category

private const val COLUMNS = 4
private const val COLLAPSED_ROWS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganiserScreen(viewModel: OrganiserViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetApp by remember { mutableStateOf<AppEntry?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 24.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(totalApps = state.totalApps, categories = state.allGroups.size) }
            item { SearchField(state.query, viewModel::setQuery) }
            item {
                CategoryChips(
                    groups = state.allGroups,
                    selected = state.selected,
                    onSelect = viewModel::select,
                )
            }

            when {
                state.loading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.groups.isEmpty() -> item { EmptyState(state.query) }
                else -> items(state.groups, key = { it.category.name }) { group ->
                    CategoryCard(
                        group = group,
                        forceExpanded = state.query.isNotBlank() || state.selected != null,
                        onLaunch = viewModel.repository::launch,
                        onLongPress = { sheetApp = it },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
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
private fun Header(totalApps: Int, categories: Int) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text("Your apps", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            if (totalApps == 0) "Sorting…" else "$totalApps apps in $categories categories",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
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
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(groups: List<CategoryGroup>, selected: Category?, onSelect: (Category?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
                shape = RoundedCornerShape(8.dp),
            )
        }
        items(groups, key = { it.category.name }) { group ->
            FilterChip(
                selected = selected == group.category,
                onClick = { onSelect(group.category) },
                label = { Text(group.category.label) },
                leadingIcon = {
                    Icon(
                        if (selected == group.category) Icons.Rounded.Check else group.category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                shape = RoundedCornerShape(8.dp),
            )
        }
    }
}

/** A category's accent, softened for dark mode so it sits nicely next to Material You colours. */
@Composable
private fun Category.tint(): Color =
    if (isSystemInDarkTheme()) lerp(accent, Color.White, 0.45f) else accent

@Composable
private fun CategoryCard(
    group: CategoryGroup,
    forceExpanded: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(group.category) { mutableStateOf(false) }
    val showAll = expanded || forceExpanded
    val limit = COLUMNS * COLLAPSED_ROWS
    val visible = if (showAll) group.apps else group.apps.take(limit)
    val tint = group.category.tint()

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
                .padding(top = 16.dp, bottom = 12.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(group.category.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(group.category.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${group.apps.size} ${if (group.apps.size == 1) "app" else "apps"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            visible.chunked(COLUMNS).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    row.forEach { app ->
                        AppTile(app, onLaunch, onLongPress, Modifier.weight(1f))
                    }
                    repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            if (!forceExpanded && group.apps.size > limit) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(if (expanded) "Show less" else "Show all ${group.apps.size}")
                    Spacer(Modifier.width(4.dp))
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
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
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(app.icon, contentDescription = null, modifier = Modifier.size(52.dp))
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

@Composable
private fun EmptyState(query: String) {
    Column(
        Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Search, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(
            if (query.isBlank()) "No apps here" else "No apps match \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleLarge)
                Text("Move to a category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
