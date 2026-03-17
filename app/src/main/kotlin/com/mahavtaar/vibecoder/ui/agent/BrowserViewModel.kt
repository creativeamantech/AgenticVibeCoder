package com.mahavtaar.vibecoder.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.vibecoder.browser.BrowsingAgent
import com.mahavtaar.vibecoder.browser.PageState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BrowserAction {
    data class Click(val selector: String) : BrowserAction()
    data class Fill(val selector: String, val value: String) : BrowserAction()
    data class Search(val query: String) : BrowserAction()
    object Screenshot : BrowserAction()
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val browsingAgent: BrowsingAgent
) : ViewModel() {

    private val _currentPage = MutableStateFlow<PageState?>(null)
    val currentPage: StateFlow<PageState?> = _currentPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun navigate(url: String) {
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                // Prepend protocol if missing
                val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                val state = browsingAgent.navigate(formattedUrl)
                _currentPage.update { state }
            } catch (e: Exception) {
                // Log or handle error implicitly
            } finally {
                _isLoading.update { false }
            }
        }
    }

    fun executeAction(action: BrowserAction) {
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                when (action) {
                    is BrowserAction.Click -> browsingAgent.clickElement(action.selector)
                    is BrowserAction.Fill -> browsingAgent.fillInput(action.selector, action.value)
                    is BrowserAction.Search -> {
                        val state = browsingAgent.navigate("https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(action.query, "UTF-8")}")
                        _currentPage.update { state }
                    }
                    is BrowserAction.Screenshot -> {
                        val bitmap = browsingAgent.captureScreenshot()
                        if (bitmap != null) {
                            _currentPage.update { it?.copy(screenshot = bitmap) }
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.update { false }
            }
        }
    }
}
