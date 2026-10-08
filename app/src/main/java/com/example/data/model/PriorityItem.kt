package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp

data class PriorityItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#3B82F6",
    val rank: Int = 50,
    val iconName: String = "flag",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toFirestoreMap(isNew: Boolean = false): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "userId" to userId,
            "name" to name.trim(),
            "colorHex" to colorHex.trim(),
            "rank" to rank,
            "iconName" to iconName
        )
        if (isNew) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        map["updatedAt"] = FieldValue.serverTimestamp()
        return map
    }

    companion object {
        val DEFAULT_PRIORITIES = listOf(
            PriorityItem(
                id = "p_critical",
                name = "Critical",
                colorHex = "#EF4444",
                rank = 90,
                iconName = "priority_high"
            ),
            PriorityItem(
                id = "p_high",
                name = "High",
                colorHex = "#F97316",
                rank = 75,
                iconName = "arrow_upward"
            ),
            PriorityItem(
                id = "p_medium",
                name = "Medium",
                colorHex = "#F59E0B",
                rank = 50,
                iconName = "remove"
            ),
            PriorityItem(
                id = "p_low",
                name = "Low",
                colorHex = "#3B82F6",
                rank = 25,
                iconName = "arrow_downward"
            ),
            PriorityItem(
                id = "p_optional",
                name = "Someday",
                colorHex = "#8B5CF6",
                rank = 10,
                iconName = "schedule"
            )
        )
    }
}
