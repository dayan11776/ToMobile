package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PriorityItem
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TodoRepository(
    private val db: FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth = Firebase.auth
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    private fun tasksCollection(userId: String) =
        db.collection("users").document(userId).collection("tasks")

    private fun prioritiesCollection(userId: String) =
        db.collection("users").document(userId).collection("priorities")

    fun observeTasks(userId: String): Flow<List<TaskItem>> = flow {
        val col = tasksCollection(userId)
        emitAll(
            col.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TaskItem::class.java)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, col.path)
                    }
                    throw error
                }
        )
    }

    fun observePriorities(userId: String): Flow<List<PriorityItem>> = flow {
        val col = prioritiesCollection(userId)
        emitAll(
            col.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PriorityItem::class.java)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, col.path)
                    }
                    throw error
                }
        )
    }

    suspend fun syncUserProfile(userId: String, email: String, displayName: String) {
        val userDoc = db.collection("users").document(userId)
        try {
            val snapshot = userDoc.get().await()
            if (!snapshot.exists()) {
                val profile = UserProfile(
                    userId = userId,
                    email = email,
                    displayName = displayName
                )
                userDoc.set(profile.toFirestoreMap(isNew = true)).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
        }
    }

    suspend fun seedDefaultPrioritiesIfEmpty(userId: String) {
        val col = prioritiesCollection(userId)
        try {
            val snapshot = col.limit(1).get().await()
            if (snapshot.isEmpty) {
                // Populate default priorities for the user
                for (item in PriorityItem.DEFAULT_PRIORITIES) {
                    val priorityWithUser = item.copy(userId = userId)
                    col.document(item.id).set(priorityWithUser.toFirestoreMap(isNew = true)).await()
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, col.path)
        }
    }

    suspend fun addTask(task: TaskItem): Result<Unit> {
        val uid = requireUserId()
        val docRef = tasksCollection(uid).document(task.id)
        return try {
            val payload = task.copy(userId = uid).toFirestoreMap(isNew = true)
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: TaskItem): Result<Unit> {
        val uid = requireUserId()
        val docRef = tasksCollection(uid).document(task.id)
        return try {
            val payload = task.copy(userId = uid).toFirestoreMap(isNew = false)
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskComplete(taskId: String, completed: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = tasksCollection(uid).document(taskId)
        return try {
            val updates = mutableMapOf<String, Any?>(
                "completed" to completed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (completed) {
                updates["completedAt"] = FieldValue.serverTimestamp()
            } else {
                updates["completedAt"] = null
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = tasksCollection(uid).document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addPriority(priority: PriorityItem): Result<Unit> {
        val uid = requireUserId()
        val docRef = prioritiesCollection(uid).document(priority.id)
        return try {
            val payload = priority.copy(userId = uid).toFirestoreMap(isNew = true)
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePriority(priority: PriorityItem): Result<Unit> {
        val uid = requireUserId()
        val docRef = prioritiesCollection(uid).document(priority.id)
        return try {
            val payload = priority.copy(userId = uid).toFirestoreMap(isNew = false)
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePriority(priorityId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = prioritiesCollection(uid).document(priorityId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
