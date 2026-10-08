package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class MainNavTab(val label: String) {
    TASKS("Tasks"),
    CALENDAR("Calendar History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedNavTab by remember { mutableStateOf(MainNavTab.TASKS) }
    var searchVisible by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var overflowMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
                viewModel.dismissError()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PrioriTask",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Live Sync pulsing dot
                            LiveSyncIndicator()
                        }
                        if (uiState.userDisplayName.isNotBlank() || uiState.userEmail.isNotBlank()) {
                            Text(
                                text = uiState.userDisplayName.ifBlank { uiState.userEmail },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) viewModel.setSearchQuery("")
                        },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Search tasks"
                        )
                    }

                    // Sort menu
                    Box {
                        IconButton(
                            onClick = { sortMenuOpen = true },
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort tasks")
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Overflow menu
                    Box {
                        IconButton(
                            onClick = { overflowMenuOpen = true },
                            modifier = Modifier.testTag("overflow_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = overflowMenuOpen,
                            onDismissRequest = { overflowMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Manage Priority Levels")
                                    }
                                },
                                onClick = {
                                    overflowMenuOpen = false
                                    viewModel.openPriorityManager()
                                },
                                modifier = Modifier.testTag("manage_priorities_menu_item")
                            )
                            if (uiState.completedCount > 0) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Clear Completed (${uiState.completedCount})")
                                        }
                                    },
                                    onClick = {
                                        overflowMenuOpen = false
                                        viewModel.clearCompletedTasks()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Sign Out")
                                    }
                                },
                                onClick = {
                                    overflowMenuOpen = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedNavTab == MainNavTab.TASKS,
                    onClick = { selectedNavTab = MainNavTab.TASKS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.activeCount > 0) {
                                    Badge { Text(uiState.activeCount.toString()) }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Checklist, contentDescription = "Tasks")
                        }
                    },
                    label = { Text("Tasks") },
                    modifier = Modifier.testTag("nav_tasks_tab")
                )
                NavigationBarItem(
                    selected = selectedNavTab == MainNavTab.CALENDAR,
                    onClick = { selectedNavTab = MainNavTab.CALENDAR },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.completedCount > 0) {
                                    Badge(containerColor = Color(0xFF10B981)) {
                                        Text(uiState.completedCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Calendar History")
                        }
                    },
                    label = { Text("Calendar History") },
                    modifier = Modifier.testTag("nav_calendar_tab")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddTaskDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add task")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add Task", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        if (selectedNavTab == MainNavTab.TASKS) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // Search Bar expand/collapse
            AnimatedVisibility(visible = searchVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search title, notes, priority, category...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_input")
                    )
                }
            }

            // Quick Stats Bar
            StatsOverviewBar(
                activeCount = uiState.activeCount,
                completedCount = uiState.completedCount,
                highPriorityCount = uiState.highPriorityPendingCount
            )

            // Status Tabs (All, Active, Done)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilter.values().forEach { filter ->
                    val isSelected = uiState.statusFilter == filter
                    val count = when (filter) {
                        StatusFilter.ALL -> uiState.tasks.size
                        StatusFilter.ACTIVE -> uiState.activeCount
                        StatusFilter.COMPLETED -> uiState.completedCount
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setStatusFilter(filter) }
                            .testTag("filter_tab_${filter.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Priority horizontal chips filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Priorities chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (uiState.selectedPriorityId == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { viewModel.togglePriorityFilter(null) }
                ) {
                    Text(
                        text = "All Priorities",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.selectedPriorityId == null) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                uiState.priorities.sortedByDescending { it.rank }.forEach { priority ->
                    val isSelected = uiState.selectedPriorityId == priority.id
                    val pColor = parseColorSafe(priority.colorHex)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) pColor.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, pColor) else null,
                        modifier = Modifier.clickable { viewModel.togglePriorityFilter(priority.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(pColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = priority.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) pColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Category horizontal chips filter if categories exist
            if (uiState.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tags:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.categories.forEach { category ->
                        val isSelected = uiState.selectedCategory.equals(category, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.clickable { viewModel.toggleCategoryFilter(category) }
                        ) {
                            Text(
                                text = "#$category",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Task list content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.filteredAndSortedTasks.isEmpty()) {
                    EmptyTaskState(
                        isFiltered = uiState.searchQuery.isNotBlank() ||
                                uiState.selectedPriorityId != null ||
                                uiState.selectedCategory != null ||
                                uiState.statusFilter != StatusFilter.ALL,
                        onClearFilters = {
                            viewModel.setSearchQuery("")
                            viewModel.togglePriorityFilter(null)
                            viewModel.toggleCategoryFilter(null)
                            viewModel.setStatusFilter(StatusFilter.ALL)
                        },
                        onAddTask = { viewModel.openAddTaskDialog() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredAndSortedTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task) },
                                onEdit = { viewModel.openEditTaskDialog(task) },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        CalendarHistoryScreen(
            tasks = uiState.tasks,
            onToggleComplete = { viewModel.toggleTaskComplete(it) },
            onEditTask = { viewModel.openEditTaskDialog(it) },
            onDeleteTask = { viewModel.deleteTask(it) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
    }

    // Dialogs
    if (uiState.isTaskDialogOpen && uiState.editingTask != null) {
        TaskDialog(
            task = uiState.editingTask!!,
            priorities = uiState.priorities,
            onDismiss = { viewModel.closeTaskDialog() },
            onSave = { updatedTask -> viewModel.saveTask(updatedTask) },
            onOpenPriorityManager = { viewModel.openPriorityManager() }
        )
    }

    if (uiState.isPriorityManagerOpen) {
        PriorityManagerDialog(
            priorities = uiState.priorities,
            onDismiss = { viewModel.closePriorityManager() },
            onAddPriority = { viewModel.openAddPriorityDialog() },
            onEditPriority = { priority -> viewModel.openEditPriorityDialog(priority) },
            onDeletePriority = { id -> viewModel.deletePriority(id) }
        )
    }

    if (uiState.isPriorityDialogOpen && uiState.editingPriority != null) {
        AddEditPriorityDialog(
            priority = uiState.editingPriority!!,
            onDismiss = { viewModel.closePriorityDialog() },
            onSave = { updatedPriority -> viewModel.savePriority(updatedPriority) }
        )
    }
}

@Composable
private fun LiveSyncIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF10B981).copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Live Sync",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = Color(0xFF059669)
            )
        }
    }
}

@Composable
private fun StatsOverviewBar(
    activeCount: Int,
    completedCount: Int,
    highPriorityCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatPill(
            label = "Active",
            count = activeCount,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "Done",
            count = completedCount,
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "Urgent/High",
            count = highPriorityCount,
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun EmptyTaskState(
    isFiltered: Boolean,
    onClearFilters: () -> Unit,
    onAddTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (isFiltered) "No matching tasks found" else "All caught up!",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isFiltered) "Try clearing some filters or searching for something else."
            else "Add a task and organize it with custom priority levels and real-time cloud sync.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (isFiltered) {
            TextButton(onClick = onClearFilters) {
                Text("Clear All Filters")
            }
        } else {
            Button(onClick = onAddTask) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Task")
            }
        }
    }
}
