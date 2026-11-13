package com.aurviz.personalagent.tools.browser

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import kotlin.coroutines.resume

class BrowserTool(context: Context) : AgentTool {
    private val appContext = context.applicationContext
    override val name = "browse_web"
    override val description = "Open a secure website and extract its visible title and text."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(
            ToolParameter("url", "string", "HTTPS URL to open"),
            ToolParameter("scroll", "string", "Optional vertical scroll amount in pixels", required = false)
        )
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val rawUrl = arguments.getValue("url").trim()
        val url = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else {
            rawUrl
        }
        val parsedUrl = Uri.parse(url)
        if (parsedUrl.scheme != "https" || parsedUrl.host.isNullOrBlank()) {
            return ToolResult(false, "", error = "Only valid HTTPS websites are allowed.")
        }
        return withContext(Dispatchers.Main) {
            val webView = WebView(appContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
            try {
                val result = withTimeout(15_000) {
                    suspendCancellableCoroutine { continuation ->
                        webView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, pageUrl: String): Boolean =
                                Uri.parse(pageUrl).scheme != "https"

                            override fun onPageFinished(view: WebView, pageUrl: String) {
                                if (!continuation.isActive) return
                                val scroll = arguments["scroll"]?.toIntOrNull()?.coerceIn(0, 10_000) ?: 0
                                view.evaluateJavascript(
                                    "(function(){window.scrollTo(0,$scroll);return JSON.stringify({title:document.title,text:document.body?document.body.innerText:''})})()"
                                ) { value ->
                                    if (continuation.isActive) {
                                        continuation.resume(JSONTokener(value).nextValue().toString())
                                    }
                                }
                            }
                        }
                        continuation.invokeOnCancellation { webView.stopLoading() }
                        webView.loadUrl(url)
                    }
                }
                ToolResult(true, result.take(MAX_OUTPUT_LENGTH), mapOf("url" to url))
            } finally {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    override fun validateArguments(arguments: Map<String, String>): String? =
        if (arguments["url"].isNullOrBlank()) "Missing required arguments: url" else null

    private companion object {
        const val MAX_OUTPUT_LENGTH = 12_000
    }
}
