package com.pranav.attendencetaker.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pranav.attendencetaker.data.model.AttendanceStatus
import com.pranav.attendencetaker.data.model.DailyLog
import com.pranav.attendencetaker.data.model.Student
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val studentsRef = db.collection("students")
    private val logsRef = db.collection("daily_logs")

    // --- STUDENT MANAGEMENT ---

    // Fetch all active students for the Card Stack
    suspend fun getActiveStudents(): List<Student> {
        return try {
            val snapshot = studentsRef
                .whereEqualTo("is_active", true)
                .orderBy("name") // Alphabetical order
                .get()
                .await()

            // Convert documents to Student objects
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Student::class.java)?.apply {
                    id = doc.id // Attach the document ID to the object
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching students", e)
            emptyList()
        }
    }

    // Add a new student (or edit existing)
    suspend fun saveStudent(student: Student): Boolean {
        return try {
            val docRef = if (student.id.isEmpty()) {
                studentsRef.document() // Create new ID
            } else {
                studentsRef.document(student.id) // Update existing
            }
            docRef.set(student).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving student", e)
            false
        }
    }

    // --- ATTENDANCE LOGIC ---

    // Get today's ID (YYYY-MM-DD) to enforce "Once a day" rule
    fun getTodayId(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Check if attendance is already started/done for today
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

    // Save the attendance stack results
    // We use SetOptions.merge() so we don't overwrite the 'focus' or other fields if they exist
    suspend fun saveAttendanceBatch(
        attendanceMap: Map<String, AttendanceStatus>,
        focusOfTheDay: String
    ) {
        val todayId = getTodayId()

        val data = hashMapOf(
            "date" to Date(),
            "attendance" to attendanceMap,
            "focus" to focusOfTheDay,
            "finalized" to false // Will be finalized by the 5 PM worker later
        )

        try {
            logsRef.document(todayId)
                .set(data, SetOptions.merge()) // Updates existing or creates new
                .await()
            Log.d("FirestoreRepo", "Attendance saved successfully for $todayId")
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error saving attendance", e)
            throw e // Rethrow to handle in UI (show error toast)
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

    // Get a specific log (already exists as getTodayLog, but let's make it generic)
    suspend fun getLogByDate(dateId: String): DailyLog? {
        return try {
            val doc = logsRef.document(dateId).get().await()
            doc.toObject(DailyLog::class.java)?.apply { id = doc.id }
        } catch (e: Exception) {
            null
        }
    }

    // Update a specific student's status in a past log
    suspend fun updateStudentStatus(dateId: String, studentId: String, status: AttendanceStatus) {
        logsRef.document(dateId)
            .update("attendance.$studentId", status) // Updates just that one field in the map
            .await()
    }
}