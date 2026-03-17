package com.mahavtaar.vibecoder.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AppDestination {
    object Editor : AppDestination()
    object Agent : AppDestination()
    object Browser : AppDestination()
    object Terminal : AppDestination()
    object Settings : AppDestination()
    object Workflows : AppDestination()
    object ModelManager : AppDestination()
    object AuditLog : AppDestination()
    object Onboarding : AppDestination()
}

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mahavtaar.vibecoder.ui.settings.appDataStore
import androidx.lifecycle.viewModelScope
import com.mahavtaar.vibecoder.error.GlobalErrorViewModel
import com.mahavtaar.vibecoder.error.AppError
import com.mahavtaar.vibecoder.util.AppUpdateChecker

@HiltViewModel
class MainViewModel @Inject constructor(
    private val context: android.content.Context,
    private val globalErrorViewModel: GlobalErrorViewModel
) : ViewModel() {

    private val _currentDestination = MutableStateFlow<AppDestination>(AppDestination.Editor)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _isOnboardingComplete = MutableStateFlow(false)
    val isOnboardingComplete: StateFlow<Boolean> = _isOnboardingComplete.asStateFlow()

    private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")

    init {
        viewModelScope.launch {
            val isComplete = context.appDataStore.data.first()[ONBOARDING_COMPLETE_KEY] ?: false
            _isOnboardingComplete.value = isComplete

            if (!isComplete) {
                _currentDestination.value = AppDestination.Onboarding
            }
        }

        viewModelScope.launch {
            val updateChecker = AppUpdateChecker()
            val latestVersion = updateChecker.checkForUpdates()
            if (latestVersion != null) {
                // Using ErrorBanner as a generic alert mechanism per instructions
                globalErrorViewModel.report(AppError.NetworkError("Update available! Latest version is $latestVersion"))
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            context.appDataStore.edit { it[ONBOARDING_COMPLETE_KEY] = true }
            _isOnboardingComplete.value = true
            _currentDestination.value = AppDestination.Editor
        }
    }

    fun navigate(destination: AppDestination) {
        _currentDestination.value = destination
    }
}
