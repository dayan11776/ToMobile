package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarHistoryFilter(val label: String) {
    COMPLETED("Completed"),
    DUE_DATES("Due Dates"),
    ALL_ACTIVITY("All Activity")
}

@Composable
fun CalendarHistoryScreen(
    tasks: List<TaskItem>,
    onToggleComplete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    var selectedDateKey by remember {
        val today = Calendar.getInstance()
        mutableStateOf(formatDateKey(today.time))
    }

    var activeFilter by remember { mutableStateOf(CalendarHistoryFilter.COMPLETED) }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayHeaderFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    // Map tasks by date keys
    val completedByDate = remember(tasks) {
        tasks.filter { it.completed }.groupBy { task ->
            val date = task.completedAt?.toDate() ?: task.updatedAt?.toDate() ?: task.createdAt?.toDate()
            if (date != null) formatDateKey(date) else ""
        }.filterKeys { it.isNotEmpty() }
    }

    val dueByDate = remember(tasks) {
        tasks.filter { it.dueDate != null }.groupBy { task ->
            formatDateKey(task.dueDate!!.toDate())
        }
    }

    val createdByDate = remember(tasks) {
        tasks.filter { it.createdAt != null }.groupBy { task ->
            formatDateKey(task.createdAt!!.toDate())
        }
    }

    // Monthly stats
    val currentMonthYear = remember(currentCalendar) {
        val cal = currentCalendar.clone() as Calendar
        cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    val monthlyCompletedCount = remember(tasks, currentMonthYear) {
        tasks.count { task ->
            if (!task.completed) return@count false
            val date = task.completedAt?.toDate() ?: task.updatedAt?.toDate()
            if (date == null) return@count false
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.YEAR) == currentMonthYear.first && cal.get(Calendar.MONTH) == currentMonthYear.second
        }
    }

    val calendarDays = remember(currentCalendar) {
        calculateMonthDays(currentCalendar)
    }

    val selectedTasks = remember(selectedDateKey, activeFilter, tasks) {
        val completed = completedByDate[selectedDateKey] ?: emptyList()
        val due = dueByDate[selectedDateKey] ?: emptyList()
        val created = createdByDate[selectedDateKey] ?: emptyList()

        when (activeFilter) {
            CalendarHistoryFilter.COMPLETED -> completed
            CalendarHistoryFilter.DUE_DATES -> due
            CalendarHistoryFilter.ALL_ACTIVITY -> (completed + due + created).distinctBy { it.id }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("calendar_history_screen")
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = monthFormat.format(currentCalendar.time),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        currentCalendar = today
                        selectedDateKey = formatDateKey(today.time)
                    }
                ) {
                    Text("Today")
                }
                IconButton(
                    onClick = {
                        val newCal = currentCalendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        currentCalendar = newCal
                    },
                    modifier = Modifier.testTag("prev_month_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                }
                IconButton(
                    onClick = {
                        val newCal = currentCalendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        currentCalendar = newCal
                    },
                    modifier = Modifier.testTag("next_month_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                }
            }
        }

        // Monthly Summary Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monthly Accomplishments",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "$monthlyCompletedCount Completed",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Weekday labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar Month Grid (Weeks)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val todayKey = formatDateKey(Calendar.getInstance().time)

            calendarDays.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { dayInfo ->
                        val isSelected = dayInfo.dateKey == selectedDateKey
                        val isToday = dayInfo.dateKey == todayKey
                        val completedList = completedByDate[dayInfo.dateKey] ?: emptyList()
                        val dueList = dueByDate[dayInfo.dateKey] ?: emptyList()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                    color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = dayInfo.isCurrentMonth) {
                                    if (dayInfo.isCurrentMonth) {
                                        selectedDateKey = dayInfo.dateKey
                                    }
                                }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayInfo.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    ),
                                    color = when {
                                        !dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                // Activity indicator dots (Completed & Due)
                                if (dayInfo.isCurrentMonth && (completedList.isNotEmpty() || dueList.isNotEmpty())) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (completedList.isNotEmpty()) {
                                            val firstPriorityColor = completedList.firstOrNull()?.priorityColor
                                            val dotColor = if (isSelected) Color.White
                                            else parseColorSafe(firstPriorityColor ?: "#10B981")
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                        if (dueList.isNotEmpty() && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalendarHistoryFilter.values().forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = { Text(filter.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Date Details Header
        val selectedDateFormatted = remember(selectedDateKey) {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDateKey)
                if (parsed != null) dayHeaderFormat.format(parsed) else selectedDateKey
            } catch (e: Exception) {
                selectedDateKey
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedDateFormatted,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${selectedTasks.size} tasks",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // List of tasks for the selected date
        if (selectedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No ${activeFilter.label.lowercase()} tasks for this date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { onToggleComplete(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

private data class DayInfo(
    val dayOfMonth: Int,
    val dateKey: String,
    val isCurrentMonth: Boolean
)

private fun formatDateKey(date: Date): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(date)
}

private fun calculateMonthDays(baseCal: Calendar): List<DayInfo> {
    val list = mutableListOf<DayInfo>()
    val cal = baseCal.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val currentMonth = cal.get(Calendar.MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.

    // Days from previous month
    val prevCal = cal.clone() as Calendar
    prevCal.add(Calendar.MONTH, -1)
    val maxDaysPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysBefore = firstDayOfWeek - Calendar.SUNDAY

    for (i in (maxDaysPrevMonth - daysBefore + 1)..maxDaysPrevMonth) {
        prevCal.set(Calendar.DAY_OF_MONTH, i)
        list.add(DayInfo(i, formatDateKey(prevCal.time), false))
    }

    // Days in current month
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, i)
        list.add(DayInfo(i, formatDateKey(cal.time), true))
    }

    // Days from next month to fill out the last week (to multiple of 7)
    val remainder = list.size % 7
    if (remainder != 0) {
        val nextCal = cal.clone() as Calendar
        nextCal.add(Calendar.MONTH, 1)
        val daysAfter = 7 - remainder
        for (i in 1..daysAfter) {
            nextCal.set(Calendar.DAY_OF_MONTH, i)
            list.add(DayInfo(i, formatDateKey(nextCal.time), false))
        }
    }

    return list
}
