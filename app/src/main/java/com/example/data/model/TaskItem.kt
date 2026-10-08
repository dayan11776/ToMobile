package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp

data class TaskItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val priorityId: String = "p_medium",
    val priorityName: String = "Medium",
    val priorityColor: String = "#F59E0B",
    val priorityRank: Int = 50,
    val category: String = "General",
    val dueDate: Timestamp? = null,
    val completedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toFirestoreMap(isNew: Boolean = false): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "id" to id,
            "userId" to userId,
            "title" to title.trim(),
            "description" to description.trim(),
            "completed" to completed,
            "priorityId" to priorityId,
            "priorityName" to priorityName,
            "priorityColor" to priorityColor,
            "priorityRank" to priorityRank,
            "category" to category.trim().ifEmpty { "General" }
        )
        if (dueDate != null) {
            map["dueDate"] = dueDate
        }
        if (completedAt != null) {
            map["completedAt"] = completedAt
        } else if (completed) {
            map["completedAt"] = FieldValue.serverTimestamp()
        }
        if (isNew) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        map["updatedAt"] = FieldValue.serverTimestamp()
        return map.filterValues { it != null }
    }
}
