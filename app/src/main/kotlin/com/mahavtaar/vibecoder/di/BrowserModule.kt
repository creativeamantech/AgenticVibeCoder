package com.mahavtaar.vibecoder.di

import android.content.Context
import android.webkit.WebView
import com.mahavtaar.vibecoder.browser.AndroidWebViewBridge
import com.mahavtaar.vibecoder.browser.BrowsingAgent
import com.mahavtaar.vibecoder.browser.HttpBrowser
import com.mahavtaar.vibecoder.browser.WebViewBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserModule {

    @Provides
    @Singleton
    fun provideHttpBrowser(): HttpBrowser {
        return HttpBrowser()
    }

    @Provides
    @Singleton
    fun provideWebViewBridge(
        @ApplicationContext context: Context,
        webViewProvider: com.mahavtaar.vibecoder.browser.WebViewProvider
    ): WebViewBridge {
        // Since WebView requires main thread and UI context,
        // it's generally risky to instantiate it in Hilt directly at startup.
        // We'll wrap it in a lazy provider bridge that retrieves it from the service.
        return object : WebViewBridge {
            private val bridge: AndroidWebViewBridge?
                get() {
                    val webView = webViewProvider.getWebView()
                    return if (webView != null) AndroidWebViewBridge(context, webView) else null
                }

            override suspend fun loadUrl(url: String): com.mahavtaar.vibecoder.browser.PageState {
                return bridge?.loadUrl(url) ?: com.mahavtaar.vibecoder.browser.PageState("", "", "", "", emptyList(), emptyList())
            }

            override suspend fun evaluateJs(js: String): String {
                return bridge?.evaluateJs(js) ?: ""
            }

            override suspend fun getPageState(): com.mahavtaar.vibecoder.browser.PageState {
                 return bridge?.getPageState() ?: com.mahavtaar.vibecoder.browser.PageState("", "", "", "", emptyList(), emptyList())
            }

            override fun clearCookies() {
                bridge?.clearCookies()
            }
        }
    }

    @Provides
    @Singleton
    fun provideBrowsingAgent(
        webViewBridge: WebViewBridge,
        httpBrowser: HttpBrowser
    ): BrowsingAgent {
        return BrowsingAgent(webViewBridge, httpBrowser)
    }
}
