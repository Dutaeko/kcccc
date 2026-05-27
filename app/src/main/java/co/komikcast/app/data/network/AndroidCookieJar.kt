package co.komikcast.app.data.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class AndroidCookieJar : CookieJar {
    private val manager = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        cookies.forEach { manager.setCookie(urlString, it.toString()) }
        manager.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return get(url)
    }

    fun get(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())
        return if (cookies.isNullOrBlank()) {
            emptyList()
        } else {
            cookies.split(";").mapNotNull { Cookie.parse(url, it.trim()) }
        }
    }

    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1): Int {
        val urlString = url.toString()
        val cookies = manager.getCookie(urlString) ?: return 0
        val names = cookies.split(";").map { it.substringBefore("=").trim() }
        val selectedNames = if (cookieNames == null) names else names.filter { it in cookieNames }
        selectedNames.forEach { manager.setCookie(urlString, "$it=;Max-Age=$maxAge") }
        manager.flush()
        return selectedNames.size
    }
}
