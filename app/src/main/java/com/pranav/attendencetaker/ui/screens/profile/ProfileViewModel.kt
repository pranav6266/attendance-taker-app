package com.pranav.attendencetaker.ui.screens.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.pranav.attendencetaker.data.AuthRepository
import com.pranav.attendencetaker.data.SettingsRepository
import com.pranav.attendencetaker.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val authRepo = AuthRepository()
    private val settingsRepo = SettingsRepository(application)
    private val storage = FirebaseStorage.getInstance()

    // UI State
    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()

    private val _profilePicUrl = MutableStateFlow<String?>(null)
    val profilePicUrl = _profilePicUrl.asStateFlow()

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme = _appTheme.asStateFlow()

    private val _morningReminder = MutableStateFlow(true)
    val morningReminder = _morningReminder.asStateFlow()

    private val _eveningReminder = MutableStateFlow(true)
    val eveningReminder = _eveningReminder.asStateFlow()

    init {
        loadUserProfile()
        observeSettings()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        _userEmail.value = user?.email ?: "No Email"
        _profilePicUrl.value = user?.photoUrl?.toString()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            launch { settingsRepo.appThemeFlow.collectLatest { _appTheme.value = it } }
            launch { settingsRepo.morningReminderFlow.collectLatest { _morningReminder.value = it } }
            launch { settingsRepo.eveningReminderFlow.collectLatest { _eveningReminder.value = it } }
        }
    }

    // --- ACTIONS ---

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepo.setAppTheme(theme) }
    }

    fun toggleMorningReminder(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setMorningReminder(enabled) }
    }

    fun toggleEveningReminder(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setEveningReminder(enabled) }
    }

    fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            try {
                // 1. Upload to Firebase Storage
                val ref = storage.reference.child("profile_pics/${user.uid}.jpg")
                ref.putFile(uri).await()

                // 2. Get Download URL
                val downloadUrl = ref.downloadUrl.await()

                // 3. Update Auth Profile
                val profileUpdates = userProfileChangeRequest {
                    photoUri = downloadUrl
                }
                user.updateProfile(profileUpdates).await()

                // 4. Update UI
                _profilePicUrl.value = downloadUrl.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        authRepo.signOut(getApplication())
        onLogoutSuccess()
    }
}