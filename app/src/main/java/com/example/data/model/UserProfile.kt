package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toFirestoreMap(isNew: Boolean = false): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "email" to email,
            "displayName" to displayName
        )
        if (isNew) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        map["updatedAt"] = FieldValue.serverTimestamp()
        return map
    }
}
