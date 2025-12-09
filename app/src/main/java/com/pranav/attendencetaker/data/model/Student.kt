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

    // phone_number <-> phoneNumber
    @get:PropertyName("phone_number")
    @set:PropertyName("phone_number")
    var phoneNumber: String = "",

    var belt: String = "White",

    // is_active <-> isActive
    @get:PropertyName("is_active")
    @set:PropertyName("is_active")
    var isActive: Boolean = true,

    // total_classes <-> totalClasses
    @get:PropertyName("total_classes")
    @set:PropertyName("total_classes")
    var totalClasses: Int = 0,

    // current_streak <-> currentStreak  (streak shown in UI)
    @get:PropertyName("current_streak")
    @set:PropertyName("current_streak")
    var currentStreak: Int = 0,

    // last_attended_date <-> lastAttendedDate
    @get:PropertyName("last_attended_date")
    @set:PropertyName("last_attended_date")
    var lastAttendedDate: Long = 0
)
