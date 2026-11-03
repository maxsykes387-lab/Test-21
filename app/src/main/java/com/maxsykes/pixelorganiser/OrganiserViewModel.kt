package com.maxsykes.pixelorganiser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxsykes.pixelorganiser.data.AppEntry
import com.maxsykes.pixelorganiser.data.AppRepository
import com.maxsykes.pixelorganiser.data.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryGroup(val category: Category, val apps: List<AppEntry>)

data class OrganiserState(
    val loading: Boolean = true,
    val query: String = "",
    val selected: Category? = null,
    val groups: List<CategoryGroup> = emptyList(),
    val allGroups: List<CategoryGroup> = emptyList(),
    val totalApps: Int = 0,
)

class OrganiserViewModel(app: Application) : AndroidViewModel(app) {

    val repository = AppRepository(app)

    private val apps = MutableStateFlow<List<AppEntry>?>(null)
    private val query = MutableStateFlow("")
    private val selected = MutableStateFlow<Category?>(null)

    val state: StateFlow<OrganiserState> =
        combine(apps, repository.overrides, query, selected) { apps, overrides, query, selected ->
            if (apps == null) return@combine OrganiserState(query = query, selected = selected)

            val all = apps
                .groupBy { overrides[it.packageName] ?: it.autoCategory }
                .let { byCategory ->
                    Category.entries.mapNotNull { cat ->
                        byCategory[cat]?.let { CategoryGroup(cat, it) }
                    }
                }

            val q = query.trim().lowercase()
            val visible = all
                .filter { selected == null || it.category == selected }
                .map { group -> group.copy(apps = group.apps.filter { q.isEmpty() || it.label.lowercase().contains(q) }) }
                .filter { it.apps.isNotEmpty() }

            OrganiserState(
                loading = false,
                query = query,
                selected = selected,
                groups = visible,
                allGroups = all,
                totalApps = apps.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganiserState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { apps.value = repository.loadApps() }
    }

    fun setQuery(value: String) { query.value = value }

    fun select(category: Category?) {
        selected.value = if (selected.value == category) null else category
    }

    fun move(app: AppEntry, category: Category) {
        viewModelScope.launch {
            repository.setOverride(app.packageName, category.takeUnless { it == app.autoCategory })
        }
    }
}
