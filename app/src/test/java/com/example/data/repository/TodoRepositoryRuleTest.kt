package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PriorityItem
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addTask_andObserve_succeedsForAuthenticatedUser() = runBlocking {
        val uid = signInTestUser("testuser1@example.com")
        println("DEBUG_UID: $uid, auth.currentUser: ${auth.currentUser?.uid}")
        val repository = TodoRepository(firestore, auth)

        val task = TaskItem(
            id = "task_test_100",
            userId = uid,
            title = "Prepare quarterly review",
            description = "Gather team notes",
            completed = false,
            priorityId = "p_high",
            priorityName = "High",
            priorityColor = "#F97316",
            priorityRank = 75,
            category = "Work"
        )

        val addResult = repository.addTask(task)
        if (addResult.isFailure) throw addResult.exceptionOrNull()!!
        assertTrue(addResult.isSuccess)

        val tasks = repository.observeTasks(uid).first()
        val found = tasks.find { it.id == "task_test_100" }
        assertNotNull(found)
        assertEquals("Prepare quarterly review", found?.title)
        assertEquals(75, found?.priorityRank)

        // Test toggle complete with completedAt
        val toggleResult = repository.toggleTaskComplete("task_test_100", true)
        assertTrue(toggleResult.isSuccess)

        val updatedTasks = repository.observeTasks(uid).first()
        val updatedTask = updatedTasks.find { it.id == "task_test_100" }
        assertTrue(updatedTask?.completed == true)
    }

    @Test
    fun addPriority_succeedsForAuthenticatedUser() = runBlocking {
        val uid = signInTestUser("testuser2@example.com")
        val repository = TodoRepository(firestore, auth)

        val priority = PriorityItem(
            id = "p_critical_test",
            userId = uid,
            name = "Mission Critical",
            colorHex = "#FF1744",
            rank = 99,
            iconName = "flag"
        )

        val result = repository.addPriority(priority)
        if (result.isFailure) throw result.exceptionOrNull()!!
        assertTrue(result.isSuccess)

        val priorities = repository.observePriorities(uid).first()
        val found = priorities.find { it.id == "p_critical_test" }
        assertNotNull(found)
        assertEquals("Mission Critical", found?.name)
        assertEquals(99, found?.rank)
    }
}
