package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PriorityItem
import com.example.data.model.TaskItem
import com.example.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class StatusFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Done")
}

enum class SortOption(val label: String) {
    PRIORITY_DESC("Priority (High to Low)"),
    DUE_DATE_ASC("Due Date (Soonest)"),
    CREATED_DESC("Recently Added"),
    TITLE_ASC("Title (A-Z)")
}

data class TodoUiState(
    val tasks: List<TaskItem> = emptyList(),
    val priorities: List<PriorityItem> = PriorityItem.DEFAULT_PRIORITIES,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val selectedPriorityId: String? = null,
    val selectedCategory: String? = null,
    val sortOption: SortOption = SortOption.PRIORITY_DESC,
    val editingTask: TaskItem? = null,
    val isTaskDialogOpen: Boolean = false,
    val isPriorityManagerOpen: Boolean = false,
    val editingPriority: PriorityItem? = null,
    val isPriorityDialogOpen: Boolean = false,
    val userEmail: String = "",
    val userDisplayName: String = "",
) {
    val categories: List<String>
        get() = tasks.map { it.category.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

    val completedCount: Int
        get() = tasks.count { it.completed }

    val activeCount: Int
        get() = tasks.count { !it.completed }

    val highPriorityPendingCount: Int
        get() = tasks.count { !it.completed && it.priorityRank >= 75 }

    val filteredAndSortedTasks: List<TaskItem>
        get() {
            return tasks
                .filter { task ->
                    // Status filter
                    when (statusFilter) {
                        StatusFilter.ALL -> true
                        StatusFilter.ACTIVE -> !task.completed
                        StatusFilter.COMPLETED -> task.completed
                    }
                }
                .filter { task ->
                    // Priority filter
                    selectedPriorityId == null || task.priorityId == selectedPriorityId
                }
                .filter { task ->
                    // Category filter
                    selectedCategory == null || task.category.equals(selectedCategory, ignoreCase = true)
                }
                .filter { task ->
                    // Search query
                    if (searchQuery.isBlank()) true
                    else {
                        val query = searchQuery.trim().lowercase()
                        task.title.lowercase().contains(query) ||
                                task.description.lowercase().contains(query) ||
                                task.category.lowercase().contains(query) ||
                                task.priorityName.lowercase().contains(query)
                    }
                }
                .sortedWith { a, b ->
                    // Completed items sorted to the bottom unless filtered to Completed
                    if (statusFilter == StatusFilter.ALL && a.completed != b.completed) {
                        return@sortedWith if (a.completed) 1 else -1
                    }

                    when (sortOption) {
                        SortOption.PRIORITY_DESC -> b.priorityRank.compareTo(a.priorityRank)
                        SortOption.DUE_DATE_ASC -> {
                            val aTime = a.dueDate?.toDate()?.time ?: Long.MAX_VALUE
                            val bTime = b.dueDate?.toDate()?.time ?: Long.MAX_VALUE
                            aTime.compareTo(bTime)
                        }
                        SortOption.CREATED_DESC -> {
                            val aTime = a.createdAt?.toDate()?.time ?: 0L
                            val bTime = b.createdAt?.toDate()?.time ?: 0L
                            bTime.compareTo(aTime)
                        }
                        SortOption.TITLE_ASC -> a.title.compareTo(b.title, ignoreCase = true)
                    }
                }
        }
}

class TodoViewModel(
    private val repository: TodoRepository,
    val userId: String,
    userEmail: String = "",
    userDisplayName: String = ""
) : ViewModel() {

    private val _rawTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    private val _rawPriorities = MutableStateFlow<List<PriorityItem>>(PriorityItem.DEFAULT_PRIORITIES)
    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val _selectedPriorityId = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(SortOption.PRIORITY_DESC)

    private val _editingTask = MutableStateFlow<TaskItem?>(null)
    private val _isTaskDialogOpen = MutableStateFlow(false)
    private val _isPriorityManagerOpen = MutableStateFlow(false)
    private val _editingPriority = MutableStateFlow<PriorityItem?>(null)
    private val _isPriorityDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<TodoUiState> = combine(
        combine(_rawTasks, _rawPriorities, _isLoading, _errorMessage) { tasks, priorities, loading, error ->
            Quad(tasks, priorities, loading, error)
        },
        combine(_searchQuery, _statusFilter, _selectedPriorityId, _selectedCategory, _sortOption) { search, status, priority, category, sort ->
            FilterBundle(search, status, priority, category, sort)
        },
        combine(_editingTask, _isTaskDialogOpen, _isPriorityManagerOpen, _editingPriority, _isPriorityDialogOpen) { editingTask, isTaskOpen, isManagerOpen, editingPriority, isPriorityOpen ->
            DialogBundle(editingTask, isTaskOpen, isManagerOpen, editingPriority, isPriorityOpen)
        }
    ) { quad, filter, dialog ->
        TodoUiState(
            tasks = quad.tasks,
            priorities = if (quad.priorities.isNotEmpty()) quad.priorities else PriorityItem.DEFAULT_PRIORITIES,
            isLoading = quad.loading,
            errorMessage = quad.error,
            searchQuery = filter.searchQuery,
            statusFilter = filter.statusFilter,
            selectedPriorityId = filter.selectedPriorityId,
            selectedCategory = filter.selectedCategory,
            sortOption = filter.sortOption,
            editingTask = dialog.editingTask,
            isTaskDialogOpen = dialog.isTaskDialogOpen,
            isPriorityManagerOpen = dialog.isPriorityManagerOpen,
            editingPriority = dialog.editingPriority,
            isPriorityDialogOpen = dialog.isPriorityDialogOpen,
            userEmail = userEmail,
            userDisplayName = userDisplayName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TodoUiState(
            userEmail = userEmail,
            userDisplayName = userDisplayName
        )
    )

    init {
        // Sync user profile and seed default priorities
        viewModelScope.launch {
            repository.syncUserProfile(userId, userEmail, userDisplayName)
            repository.seedDefaultPrioritiesIfEmpty(userId)
        }

        // Real-time observation of tasks
        viewModelScope.launch {
            try {
                repository.observeTasks(userId).collect { taskList ->
                    _rawTasks.value = taskList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to sync tasks"
                _isLoading.value = false
            }
        }

        // Real-time observation of priorities
        viewModelScope.launch {
            try {
                repository.observePriorities(userId).collect { priorityList ->
                    if (priorityList.isNotEmpty()) {
                        // Merge or replace with user's stored priorities
                        _rawPriorities.value = priorityList.sortedByDescending { it.rank }
                    }
                }
            } catch (e: Exception) {
                // Priority stream error fallback
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun togglePriorityFilter(priorityId: String?) {
        _selectedPriorityId.value = if (_selectedPriorityId.value == priorityId) null else priorityId
    }

    fun toggleCategoryFilter(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun openAddTaskDialog() {
        val defaultPriority = _rawPriorities.value.firstOrNull() ?: PriorityItem.DEFAULT_PRIORITIES[2]
        _editingTask.value = TaskItem(
            id = UUID.randomUUID().toString(),
            userId = userId,
            priorityId = defaultPriority.id,
            priorityName = defaultPriority.name,
            priorityColor = defaultPriority.colorHex,
            priorityRank = defaultPriority.rank
        )
        _isTaskDialogOpen.value = true
    }

    fun openEditTaskDialog(task: TaskItem) {
        _editingTask.value = task
        _isTaskDialogOpen.value = true
    }

    fun closeTaskDialog() {
        _isTaskDialogOpen.value = false
        _editingTask.value = null
    }

    fun saveTask(task: TaskItem) {
        viewModelScope.launch {
            val isNew = !_rawTasks.value.any { it.id == task.id }
            val result = if (isNew) {
                repository.addTask(task)
            } else {
                repository.updateTask(task)
            }
            if (result.isSuccess) {
                closeTaskDialog()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save task"
            }
        }
    }

    fun toggleTaskComplete(task: TaskItem) {
        viewModelScope.launch {
            val newCompleted = !task.completed
            // Optimistic update
            _rawTasks.value = _rawTasks.value.map {
                if (it.id == task.id) it.copy(completed = newCompleted) else it
            }
            val result = repository.toggleTaskComplete(task.id, newCompleted)
            if (result.isFailure) {
                // Revert on failure
                _rawTasks.value = _rawTasks.value.map {
                    if (it.id == task.id) it.copy(completed = task.completed) else it
                }
                _errorMessage.value = "Failed to update task state"
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val previousTasks = _rawTasks.value
            _rawTasks.value = _rawTasks.value.filter { it.id != taskId }
            val result = repository.deleteTask(taskId)
            if (result.isFailure) {
                _rawTasks.value = previousTasks
                _errorMessage.value = "Failed to delete task"
            }
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val completedTasks = _rawTasks.value.filter { it.completed }
            for (task in completedTasks) {
                repository.deleteTask(task.id)
            }
        }
    }

    fun openPriorityManager() {
        _isPriorityManagerOpen.value = true
    }

    fun closePriorityManager() {
        _isPriorityManagerOpen.value = false
    }

    fun openAddPriorityDialog() {
        _editingPriority.value = PriorityItem(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            name = "",
            colorHex = "#3B82F6",
            rank = 50,
            iconName = "flag"
        )
        _isPriorityDialogOpen.value = true
    }

    fun openEditPriorityDialog(priority: PriorityItem) {
        _editingPriority.value = priority
        _isPriorityDialogOpen.value = true
    }

    fun closePriorityDialog() {
        _isPriorityDialogOpen.value = false
        _editingPriority.value = null
    }

    fun savePriority(priority: PriorityItem) {
        viewModelScope.launch {
            val isNew = !_rawPriorities.value.any { it.id == priority.id }
            val result = if (isNew) {
                repository.addPriority(priority)
            } else {
                repository.updatePriority(priority)
            }

            if (result.isSuccess) {
                closePriorityDialog()
                // Update any existing tasks with this priority's updated attributes
                val updatedTasks = _rawTasks.value.filter { it.priorityId == priority.id }
                for (task in updatedTasks) {
                    val updatedTask = task.copy(
                        priorityName = priority.name,
                        priorityColor = priority.colorHex,
                        priorityRank = priority.rank
                    )
                    repository.updateTask(updatedTask)
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save priority"
            }
        }
    }

    fun deletePriority(priorityId: String) {
        viewModelScope.launch {
            val defaultPriority = PriorityItem.DEFAULT_PRIORITIES[2] // Medium
            val result = repository.deletePriority(priorityId)
            if (result.isSuccess) {
                // Reassign any tasks using this priority to default Medium
                val affectedTasks = _rawTasks.value.filter { it.priorityId == priorityId }
                for (task in affectedTasks) {
                    val fallbackTask = task.copy(
                        priorityId = defaultPriority.id,
                        priorityName = defaultPriority.name,
                        priorityColor = defaultPriority.colorHex,
                        priorityRank = defaultPriority.rank
                    )
                    repository.updateTask(fallbackTask)
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete priority"
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    private data class Quad<A, B, C, D>(val tasks: A, val priorities: B, val loading: C, val error: D)
    private data class FilterBundle(
        val searchQuery: String,
        val statusFilter: StatusFilter,
        val selectedPriorityId: String?,
        val selectedCategory: String?,
        val sortOption: SortOption
    )
    private data class DialogBundle(
        val editingTask: TaskItem?,
        val isTaskDialogOpen: Boolean,
        val isPriorityManagerOpen: Boolean,
        val editingPriority: PriorityItem?,
        val isPriorityDialogOpen: Boolean
    )
}
