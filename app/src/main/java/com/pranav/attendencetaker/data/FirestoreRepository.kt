package com.pranav.attendencetaker.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.data.model.Student
import com.pranav.attendencetaker.domain.CalculateStreakUseCase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val studentsRef = db.collection("students")
    private val logsRef = db.collection("daily_logs")

    // --- STUDENT MANAGEMENT ---

    fun getActiveStudentsFlow(): Flow<List<Student>> = callbackFlow {
        val listener = studentsRef
            .whereEqualTo("is_active", true)
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val students = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Student::class.java)?.apply {
                            id = doc.id
                        }
                    }
                    trySend(students)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getActiveStudents(): List<Student> {
        return try {
            val snapshot = studentsRef
                .whereEqualTo("is_active", true)
                .orderBy("name")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Student::class.java)?.apply {
                    id = doc.id
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching students", e)
            emptyList()
        }
    }

    suspend fun getStudentById(studentId: String): Student? {
        return try {
            val doc = studentsRef.document(studentId).get().await()
            doc.toObject(Student::class.java)?.apply {
                id = doc.id
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveStudent(student: Student): Boolean {
        return try {
            val docRef = if (student.id.isEmpty()) {
                studentsRef.document()
            } else {
                studentsRef.document(student.id)
            }
            docRef.set(student).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving student", e)
            false
        }
    }

    suspend fun deleteStudent(studentId: String): Boolean {
        return try {
            studentsRef.document(studentId)
                .update("is_active", false)
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error deleting student", e)
            false
        }
    }

    // --- ATTENDANCE LOGIC ---

    fun getTodayId(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    suspend fun getTodayLog(): DailyLog? {
        val todayId = getTodayId()
        return try {
            val doc = logsRef.document(todayId).get().await()
            if (doc.exists()) {
                doc.toObject(DailyLog::class.java)?.apply { id = doc.id }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveAttendanceBatch(
        attendanceMap: Map<String, AttendanceStatus>,
        focusOfTheDay: String
    ) {
        val todayId = getTodayId()

        val data = hashMapOf(
            "date" to Date(),
            "attendance" to attendanceMap,
            "focus" to focusOfTheDay,
        )

        try {
            logsRef.document(todayId)
                .set(data, SetOptions.merge())
                .await()
            Log.d("FirestoreRepo", "Attendance saved successfully for $todayId")
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving attendance", e)
            throw e
        }
    }

    suspend fun getAllLogs(): List<DailyLog> {
        return try {
            val snapshot = logsRef.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(DailyLog::class.java)?.apply { id = doc.id }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLogByDate(dateId: String): DailyLog? {
        return try {
            val doc = logsRef.document(dateId).get().await()
            doc.toObject(DailyLog::class.java)?.apply { id = doc.id }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateStudentStatus(dateId: String, studentId: String, status: AttendanceStatus) {
        logsRef.document(dateId)
            .update("attendance.$studentId", status)
            .await()
    }

    // --- STREAK RECALCULATION (FIX) ---
    suspend fun recalculateStreaks(students: List<Student>): List<Student> {
        val calculateStreak = CalculateStreakUseCase()
        val allLogs = getAllLogs() // Fetch history once
        val batch = db.batch()
        var batchCount = 0

        // Create a mutable copy of the list to return updated values to UI immediately
        val updatedStudentList = students.map { it.copy() }.toMutableList()

        updatedStudentList.forEachIndexed { index, student ->
            // 1. Collect all dates this student was present/late
            val attendedDates = allLogs.filter { log ->
                val status = log.attendance[student.id]
                status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE
            }.map { it.date }

            // 2. Calculate true streak
            val newStreak = calculateStreak(attendedDates)

            // 3. If different from Firestore, queue update
            if (newStreak != student.currentStreak) {
                val ref = studentsRef.document(student.id)
                batch.update(ref, "current_streak", newStreak)
                batchCount++

                // Update local object so UI reflects it immediately
                updatedStudentList[index].currentStreak = newStreak
            }
        }

        // 4. Commit updates if needed
        if (batchCount > 0) {
            try {
                batch.commit().await()
                Log.d("FirestoreRepo", "Corrected streaks for $batchCount students")
            } catch (e: Exception) {
                Log.e("FirestoreRepo", "Error committing streak corrections", e)
            }
        }

        return updatedStudentList
    }

    suspend fun finalizeLog(dateId: String) {
        try {
            logsRef.document(dateId)
                .update("finalized", true)
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error finalizing log", e)
            throw e
        }
    }

    suspend fun updateFocus(dateId: String, newFocus: String) {
        try {
            logsRef.document(dateId)
                .update("focus", newFocus)
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error updating focus", e)
            throw e
        }
    }
}