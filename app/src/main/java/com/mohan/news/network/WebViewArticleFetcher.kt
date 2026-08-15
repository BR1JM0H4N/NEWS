package com.mohan.news.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine

data class WebViewLoadResult(val finalUrl: String, val html: String)

/**
 * Loads a page in an invisible, never-attached WebView so that any
 * client-side JavaScript actually runs — Google News' redirect script,
 * cookie-consent gates, single-page-app hydration, etc. — the same way a
 * real browser tab would, then hands back the settled URL and the fully
 * rendered HTML for extraction.
 */
@SuppressLint("SetJavaScriptEnabled")
object WebViewArticleFetcher {

    private const val SETTLE_DELAY_MS = 1400L
    private const val OVERALL_TIMEOUT_MS = 20_000L
    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    suspend fun load(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit = {}
    ): WebViewLoadResult? =
        suspendCancellableCoroutine { cont ->
            val mainHandler = Handler(Looper.getMainLooper())

            mainHandler.post {
                var resolved = false
                var webView: WebView? = null

                fun cleanupAndResume(result: WebViewLoadResult?) {
                    if (resolved) return
                    resolved = true
                    val toDestroy = webView
                    webView = null
                    mainHandler.post {
                        toDestroy?.apply {
                            stopLoading()
                            webViewClient = WebViewClient()
                            destroy()
                        }
                    }
                    if (cont.isActive) cont.resumeWith(Result.success(result))
                }

                fun captureCurrentState(view: WebView) {
                    val landedUrl = view.url ?: url
                    view.evaluateJavascript("document.documentElement.outerHTML") { rawJs ->
                        cleanupAndResume(WebViewLoadResult(landedUrl, decodeJsString(rawJs)))
                    }
                }

                val timeoutRunnable = Runnable {
                    webView?.let { captureCurrentState(it) } ?: cleanupAndResume(null)
                }
                mainHandler.postDelayed(timeoutRunnable, OVERALL_TIMEOUT_MS)

                try {
                    webView = WebView(context.applicationContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = UA
                        settings.loadsImagesAutomatically = false
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (!resolved) onProgress(newProgress)
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, loadedUrl: String) {
                                super.onPageFinished(view, loadedUrl)
                                if (resolved) return
                                if (loadedUrl.contains("news.google.")) {
                                    // Likely still on Google's interstitial — its own
                                    // redirect script may fire shortly; keep waiting
                                    // (the overall timeout will still catch us if not).
                                    return
                                }
                                // Looks like we've landed on the real publisher page.
                                // Give client-rendered content a moment to hydrate.
                                mainHandler.postDelayed({
                                    if (!resolved) captureCurrentState(view)
                                }, SETTLE_DELAY_MS)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                super.onReceivedError(view, request, error)
                                // Only bail out on a failed main-frame navigation;
                                // sub-resource errors (blocked images, trackers, etc.)
                                // are expected and harmless here.
                                if (!resolved && request.isForMainFrame) {
                                    mainHandler.removeCallbacks(timeoutRunnable)
                                    captureCurrentState(view)
                                }
                            }
                        }
                        loadUrl(url)
                    }
                } catch (e: Exception) {
                    cleanupAndResume(null)
                }

                cont.invokeOnCancellation {
                    mainHandler.post {
                        webView?.apply {
                            stopLoading()
                            webViewClient = WebViewClient()
                            destroy()
                        }
                        webView = null
                    }
                }
            }
        }

    /** evaluateJavascript() hands back a JSON-encoded string literal; unwrap it. */
    private fun decodeJsString(raw: String?): String {
        if (raw.isNullOrEmpty() || raw == "null") return ""
        return try {
            org.json.JSONTokener(raw).nextValue() as? String ?: raw
        } catch (e: Exception) {
            raw.trim('"')
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\")
        }
    }
}
