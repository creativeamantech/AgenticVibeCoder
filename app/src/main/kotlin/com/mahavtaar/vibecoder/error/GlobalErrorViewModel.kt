package com.mahavtaar.vibecoder.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalErrorViewModel @Inject constructor() : ViewModel() {
    private val _errors = MutableSharedFlow<AppError>(replay = 1)
    val errors: SharedFlow<AppError> = _errors.asSharedFlow()

    fun report(error: AppError) {
        viewModelScope.launch {
            _errors.emit(error)
        }
    }
}
