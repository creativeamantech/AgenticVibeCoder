package com.mahavtaar.vibecoder.browser

import android.graphics.Bitmap

data class PageLink(
    val text: String,
    val href: String,
    val id: String
)

data class PageInput(
    val type: String,
    val name: String,
    val id: String,
    val placeholder: String,
    val value: String
)

data class PageState(
    val url: String,
    val title: String,
    val rawHtml: String,
    val visibleText: String,
    val links: List<PageLink>,
    val inputs: List<PageInput>,
    val screenshot: Bitmap? = null
)

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)
