package com.pranav.attendencetaker.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

enum class AttendanceStatus {
    PRESENT, ABSENT, LATE
}

data class Student(
    @get:Exclude var id: String = "",

    val name: String = "",
    val age: Int = 0,

    // CHANGE 1: val -> var, remove 'get:'
    @PropertyName("phone_number")
    var phoneNumber: String = "",

    var belt: String = "White",

    // CHANGE 2: val -> var, remove 'get:'
    @PropertyName("is_active")
    var isActive: Boolean = true,

    // CHANGE 3: val -> var, remove 'get:'
    @PropertyName("total_classes")
    var totalClasses: Int = 0,

    // CHANGE 4: val -> var, remove 'get:' -- THIS FIXES YOUR STREAK BUG
    @PropertyName("current_streak")
    var currentStreak: Int = 0,

    // CHANGE 5: val -> var, remove 'get:'
    @PropertyName("last_attended_date")
    var lastAttendedDate: Long = 0
)