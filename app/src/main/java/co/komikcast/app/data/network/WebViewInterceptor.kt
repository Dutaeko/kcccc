package co.komikcast.app.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class WebViewInterceptor(
    private val context: Context,
    private val defaultUserAgentProvider: () -> String
) : Interceptor {
    private val initWebView by lazy {
        try {
            WebSettings.getDefaultUserAgent(context)
        } catch (_: Exception) {
        }
    }

    abstract fun shouldIntercept(response: Response): Boolean

    abstract fun intercept(chain: Interceptor.Chain, request: Request, response: Response): Response

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!shouldIntercept(response)) return response
        if (!supportsWebView()) return response
        initWebView
        return intercept(chain, request, response)
    }

    fun parseHeaders(headers: Headers): Map<String, String> {
        return headers
            .filter { (name, value) -> isRequestHeaderSafe(name, value) }
            .groupBy(keySelector = { (name, _) -> name }) { (_, value) -> value }
            .mapValues { it.value.firstOrNull().orEmpty() }
    }

    fun CountDownLatch.awaitFor30Seconds() {
        await(30, TimeUnit.SECONDS)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(request: Request): WebView {
        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.setSupportMultipleWindows(true)
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.userAgentString = request.header("User-Agent") ?: defaultUserAgentProvider()
            CookieManager.getInstance().setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }
        }
    }

    private fun supportsWebView(): Boolean {
        return try {
            CookieManager.getInstance()
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)
        } catch (_: Throwable) {
            false
        }
    }
}

private fun isRequestHeaderSafe(rawName: String, rawValue: String): Boolean {
    val name = rawName.lowercase(Locale.ENGLISH)
    val value = rawValue.lowercase(Locale.ENGLISH)
    if (name in unsafeHeaderNames || name.startsWith("proxy-")) return false
    if (name == "connection" && value == "upgrade") return false
    return true
}

private val unsafeHeaderNames = listOf(
    "content-length",
    "host",
    "trailer",
    "te",
    "upgrade",
    "cookie2",
    "keep-alive",
    "transfer-encoding",
    "set-cookie"
)
