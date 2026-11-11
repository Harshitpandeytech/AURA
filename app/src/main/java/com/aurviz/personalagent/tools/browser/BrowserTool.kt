package com.aurviz.personalagent.tools.browser

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
        val url = arguments.getValue("url")
        if (!url.startsWith("https://")) return ToolResult(false, "", error = "Only HTTPS websites are allowed.")
        return withContext(Dispatchers.Main) {
            val webView = WebView(appContext).apply {
                settings.javaScriptEnabled = false
            }
            val result = suspendCancellableCoroutine { continuation ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, pageUrl: String) {
                        val scroll = arguments["scroll"]?.toIntOrNull()?.coerceIn(0, 10000) ?: 0
                        view.evaluateJavascript(
                            "(function(){window.scrollTo(0,$scroll);return JSON.stringify({title:document.title,text:document.body?document.body.innerText:''})})()"
                        ) { value ->
                            continuation.resume(value.removeSurrounding("\"").replace("\\\"", "\""))
                        }
                    }
                }
                continuation.invokeOnCancellation { webView.stopLoading(); webView.destroy() }
                webView.loadUrl(url)
            }
            webView.destroy()
            ToolResult(true, result.take(12000), mapOf("url" to url))
        }
    }

    override fun validateArguments(arguments: Map<String, String>): String? =
        if (arguments["url"].isNullOrBlank()) "Missing required arguments: url" else null
}
