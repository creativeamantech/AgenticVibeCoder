package com.mahavtaar.vibecoder.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class AppDestination {
    object Editor : AppDestination()
    object Agent : AppDestination()
    object Browser : AppDestination()
    object Terminal : AppDestination()
    object Settings : AppDestination()
    object Workflows : AppDestination()
    object ModelManager : AppDestination()
}

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _currentDestination = MutableStateFlow<AppDestination>(AppDestination.Editor)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    fun navigate(destination: AppDestination) {
        _currentDestination.value = destination
    }
}
