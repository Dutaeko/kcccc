package co.komikcast.app.data.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.CountDownLatch

class CloudflareInterceptor(
    context: Context,
    private val cookieJar: AndroidCookieJar,
    defaultUserAgentProvider: () -> String
) : WebViewInterceptor(context, defaultUserAgentProvider) {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun shouldIntercept(response: Response): Boolean {
        if (response.code !in errorCodes) return false
        val server = response.header("Server").orEmpty().lowercase()
        val cloudflareServer = server.contains("cloudflare") || server.contains("cloudflare-nginx")
        val body = response.peekBody(1024L * 1024L).string()
        val document = Jsoup.parse(body, response.request.url.toString())
        val challengeTitle = document.getElementById("challenge-error-title") != null
        val challengeText = document.getElementById("challenge-error-text") != null
        val challengeBody = body.contains("cf_chl", ignoreCase = true) || body.contains("cf_clearance", ignoreCase = true)
        return cloudflareServer || challengeTitle || challengeText || challengeBody
    }

    override fun intercept(chain: Interceptor.Chain, request: Request, response: Response): Response {
        try {
            response.close()
            cookieJar.remove(request.url, cookieNames, 0)
            val oldCookie = cookieJar.get(request.url).firstOrNull { it.name == "cf_clearance" }
            resolveWithWebView(request, oldCookie)
            return chain.proceed(request)
        } catch (e: CloudflareBypassException) {
            throw IOException("Gagal melewati proteksi Cloudflare", e)
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    private fun resolveWithWebView(originalRequest: Request, oldCookie: Cookie?) {
        val latch = CountDownLatch(1)
        var webView: WebView? = null
        var challengeFound = false
        var cloudflareBypassed = false
        val requestUrl = originalRequest.url.toString()
        val headers = parseHeaders(originalRequest.headers)

        mainHandler.post {
            webView = createWebView(originalRequest)
            webView?.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val newCookie = cookieJar.get(requestUrl.toHttpUrl()).firstOrNull { it.name == "cf_clearance" }
                    if (newCookie != null && newCookie != oldCookie) {
                        cloudflareBypassed = true
                        latch.countDown()
                    }
                    if (url == requestUrl && !challengeFound) {
                        latch.countDown()
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true) {
                        if (errorResponse?.statusCode in errorCodes) {
                            challengeFound = true
                        } else {
                            latch.countDown()
                        }
                    }
                }
            }
            webView?.loadUrl(requestUrl, headers)
        }

        latch.awaitFor30Seconds()

        mainHandler.post {
            webView?.stopLoading()
            webView?.destroy()
        }

        if (!cloudflareBypassed) throw CloudflareBypassException()
    }
}

private val errorCodes = listOf(403, 503)
private val cookieNames = listOf("cf_clearance")
private class CloudflareBypassException : Exception()
