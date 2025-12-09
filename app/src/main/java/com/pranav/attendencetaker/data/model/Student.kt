package com.pranav.attendencetaker.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

// 1. Enum for strict type safety (prevents typos like "Present" vs "present")
enum class AttendanceStatus {
    PRESENT, ABSENT, LATE
}

// 2. The Student Data Class
data class Student(
    // We explicitly exclude ID from the document body because it is the Document Name itself
    @get:Exclude var id: String = "",

    val name: String = "",
    val age: Int = 0,

    @get:PropertyName("phone_number") // Maps to "phone_number" in Firestore
    val phoneNumber: String = "",

    val belt: String = "White", // Default belt

    @get:PropertyName("is_active")
    val isActive: Boolean = true, // To filter out dropouts

    // Quick Stats for the UI
    @get:PropertyName("total_classes")
    val totalClasses: Int = 0,

    @get:PropertyName("current_streak")
    val currentStreak: Int = 0
)