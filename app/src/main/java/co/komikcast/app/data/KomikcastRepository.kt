package co.komikcast.app.data

import android.content.ContentValues
import android.content.Context
import android.webkit.WebSettings
import co.komikcast.app.data.network.AndroidCookieJar
import co.komikcast.app.data.network.CloudflareInterceptor
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.math.roundToInt

class KomikcastRepository(
    private val context: Context,
    private val dao: MangaDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private val cache = Cache(File(context.cacheDir, "komikcast_http_cache"), 64L * 1024L * 1024L)
    private val cookieJar = AndroidCookieJar()
    private val userAgent = defaultUserAgent()
    private val client = OkHttpClient.Builder()
        .cache(cache)
        .cookieJar(cookieJar)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", userAgent)
                .header("Referer", "https://v2.komikcast.fit/")
                .header("Origin", "https://v2.komikcast.fit")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.7,en;q=0.6")
                .header("Cache-Control", "max-age=600")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(CloudflareInterceptor(context, cookieJar) { userAgent })
        .build()

    fun observeFavorites(): Flow<List<FavoriteMangaEntity>> = dao.observeFavorites()

    fun observeFavorite(slug: String): Flow<FavoriteMangaEntity?> = dao.observeFavorite(slug)

    fun observeLatestHistories(): Flow<List<ReadingHistoryEntity>> = dao.observeLatestHistories()

    fun observeHistoriesForManga(slug: String): Flow<List<ReadingHistoryEntity>> = dao.observeHistoriesForManga(slug)

    suspend fun latest(page: Int): MangaPage = fetchSeries(page, MangaFeedType.Latest, null, null)

    suspend fun popular(page: Int): MangaPage = fetchSeries(page, MangaFeedType.Popular, null, null)

    suspend fun search(page: Int, query: String, genreName: String?): MangaPage = fetchSeries(page, MangaFeedType.Search, query, genreName)

    suspend fun genre(page: Int, genreName: String): MangaPage = fetchSeries(page, MangaFeedType.Search, null, genreName)

    suspend fun genres(): List<GenreItem> = awaitFuture {
        val json = requestJson("https://be.komikcast.cc/genres")
        val array = json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val data = item.optJSONObject("data") ?: JSONObject()
                val name = data.optString("name")
                if (name.isNotBlank()) add(GenreItem(item.optInt("id"), name))
            }
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    suspend fun detail(slug: String): MangaDetail = awaitFuture {
        val json = requestJson("https://be.komikcast.cc/series/$slug")
        val wrapper = json.optJSONObject("data") ?: JSONObject()
        val manga = parseManga(wrapper)
        val info = listOf(
            "Author" to manga.author,
            "Format" to manga.format,
            "Status" to manga.status,
            "Rating" to manga.rating,
            "Native Title" to manga.nativeTitle,
            "Release" to manga.releaseDate,
            "Total Chapter" to manga.totalChapters
        )
        MangaDetail(manga, info)
    }

    suspend fun chapters(slug: String): List<ChapterItem> = awaitFuture {
        val json = requestJson("https://be.komikcast.cc/series/$slug/chapters")
        val array = json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val data = item.optJSONObject("data") ?: JSONObject()
                val index = parseChapterIndex(data.opt("index"))
                val rawTitle = cleanText(data.opt("title")?.toString().orEmpty())
                val releaseDate = formatReleaseDate(item.optString("createdAt"))
                add(ChapterItem(item.optLong("id"), index, rawTitle, releaseDate))
            }
        }.sortedByDescending { numericChapterIndex(it.index) }
    }

    suspend fun chapter(slug: String, chapterIndex: String, seriesTitle: String, coverImage: String): ReaderChapter = awaitFuture {
        val chapterList = chaptersBlocking(slug)
        val requestedIndex = normalizeChapterIndex(chapterIndex)
        val url = "https://be.komikcast.cc/series/$slug/chapters/$requestedIndex"
        val json = requestJson(url)
        val wrapper = json.optJSONObject("data") ?: JSONObject()
        val data = wrapper.optJSONObject("data") ?: JSONObject()
        val images = data.optJSONArray("images") ?: JSONArray()
        val chapterTitle = cleanText(data.optString("title")).ifBlank { "Chapter ${displayChapterIndex(requestedIndex)}" }
        ReaderChapter(
            seriesSlug = slug,
            seriesTitle = seriesTitle,
            coverImage = coverImage,
            chapterIndex = requestedIndex,
            chapterTitle = chapterTitle,
            images = buildList {
                for (i in 0 until images.length()) {
                    val image = images.optString(i)
                    if (image.isNotBlank()) add(image)
                }
            },
            chapters = chapterList.sortedBy { numericChapterIndex(it.index) }
        )
    }

    suspend fun toggleFavorite(manga: MangaItem) {
        withContext(dispatcher) {
            val current = dao.getFavorite(manga.slug)
            if (current == null) {
                dao.upsertFavorite(
                    FavoriteMangaEntity(
                        slug = manga.slug,
                        title = manga.title,
                        coverImage = manga.coverImage,
                        latestChapter = manga.latestChapter,
                        addedAt = System.currentTimeMillis()
                    )
                )
            } else {
                dao.deleteFavorite(current)
            }
        }
    }

    suspend fun exportFavoritesEncrypted(): String = withContext(dispatcher) {
        val array = JSONArray()
        dao.getFavorites().forEach { item ->
            array.put(
                JSONObject()
                    .put("slug", item.slug)
                    .put("title", item.title)
                    .put("coverImage", item.coverImage)
                    .put("latestChapter", item.latestChapter)
                    .put("addedAt", item.addedAt)
            )
        }
        encryptText(array.toString())
    }

    suspend fun importFavoritesEncrypted(value: String): Int = withContext(dispatcher) {
        val decoded = decryptText(value.trim())
        val array = JSONArray(decoded)
        val list = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val slug = item.optString("slug")
                val title = item.optString("title")
                val coverImage = item.optString("coverImage")
                if (slug.isBlank() || title.isBlank()) continue
                add(
                    FavoriteMangaEntity(
                        slug = slug,
                        title = title,
                        coverImage = coverImage,
                        latestChapter = item.optString("latestChapter"),
                        addedAt = item.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
        }
        dao.upsertFavorites(list)
        list.size
    }

    suspend fun clearHistories() {
        withContext(dispatcher) { dao.clearHistories() }
    }

    suspend fun deleteHistories(slugs: List<String>) {
        withContext(dispatcher) {
            if (slugs.isNotEmpty()) dao.deleteHistoriesBySeries(slugs.distinct())
        }
    }

    suspend fun startChapterFor(slug: String, availableChapters: List<ChapterItem>): ChapterItem? {
        return withContext(dispatcher) {
            val firstChapter = availableChapters.minByOrNull { numericChapterIndex(it.index) }
            val history = dao.getLastHistory(slug)
            if (history != null) {
                availableChapters.firstOrNull { numericChapterIndex(it.index) == numericChapterIndex(history.chapterIndex) } ?: firstChapter
            } else {
                firstChapter
            }
        }
    }

    suspend fun markHistory(reader: ReaderChapter, visibleImageIndex: Int) {
        withContext(dispatcher) {
            val total = reader.images.size.coerceAtLeast(1)
            val current = visibleImageIndex.coerceIn(0, total - 1)
            val progress = (((current + 1).toFloat() / total.toFloat()) * 100f).roundToInt().coerceIn(1, 100)
            dao.upsertHistory(
                ReadingHistoryEntity(
                    seriesSlug = reader.seriesSlug,
                    seriesTitle = reader.seriesTitle,
                    coverImage = reader.coverImage,
                    chapterIndex = reader.chapterIndex,
                    chapterTitle = reader.chapterTitle,
                    progress = progress,
                    lastImageIndex = current,
                    totalImages = total,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun saveCover(manga: MangaItem): Boolean = awaitFuture {
        val request = Request.Builder().url(manga.coverImage).build()
        val bytes = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@awaitFuture false
            response.body?.bytes() ?: return@awaitFuture false
        }
        val fileName = manga.title.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { manga.slug } + ".webp"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Komikcast")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@awaitFuture false
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@awaitFuture false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Komikcast")
            if (!dir.exists()) dir.mkdirs()
            File(dir, fileName).writeBytes(bytes)
            true
        }
    }

    private suspend fun fetchSeries(page: Int, type: MangaFeedType, query: String?, genreName: String?): MangaPage = awaitFuture {
        val builder = "https://be.komikcast.cc/series".toHttpUrl().newBuilder()
            .addQueryParameter("includeMeta", "true")
            .addQueryParameter("take", "12")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("sortOrder", "desc")
        when (type) {
            MangaFeedType.Latest -> builder.addQueryParameter("sort", "latest")
            MangaFeedType.Popular -> builder.addQueryParameter("sort", "popularity")
            MangaFeedType.Search -> builder.addQueryParameter("sort", "popular")
        }
        val normalizedQuery = query?.trim().orEmpty()
        if (normalizedQuery.isNotBlank()) {
            builder.addQueryParameter("filter", "title=like=\"$normalizedQuery\",nativeTitle=like=\"$normalizedQuery\"")
        }
        val normalizedGenre = genreName?.trim().orEmpty()
        if (normalizedGenre.isNotBlank()) {
            builder.addQueryParameter("genreIds", resolveGenreId(normalizedGenre))
        }
        val json = requestJson(builder.build().toString())
        val array = json.optJSONArray("data") ?: JSONArray()
        val meta = json.optJSONObject("meta") ?: JSONObject()
        MangaPage(
            items = buildList {
                for (i in 0 until array.length()) {
                    add(parseManga(array.optJSONObject(i) ?: JSONObject()))
                }
            },
            page = meta.optInt("page", page),
            lastPage = meta.optInt("lastPage", page)
        )
    }

    private fun chaptersBlocking(slug: String): List<ChapterItem> {
        val json = requestJson("https://be.komikcast.cc/series/$slug/chapters")
        val array = json.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val data = item.optJSONObject("data") ?: JSONObject()
                val index = parseChapterIndex(data.opt("index"))
                val rawTitle = cleanText(data.opt("title")?.toString().orEmpty())
                val releaseDate = formatReleaseDate(item.optString("createdAt"))
                add(ChapterItem(item.optLong("id"), index, rawTitle, releaseDate))
            }
        }
    }

    private fun resolveGenreId(value: String): String {
        if (value.toIntOrNull() != null) return value
        val json = requestJson("https://be.komikcast.cc/genres")
        val array = json.optJSONArray("data") ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val data = item.optJSONObject("data") ?: JSONObject()
            val name = data.optString("name")
            if (name.equals(value, ignoreCase = true)) {
                return item.optInt("id").toString()
            }
        }
        return value
    }

    private fun requestJson(url: String): JSONObject {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Gagal mengambil data ${response.code}")
            val raw = response.body?.string().orEmpty()
            val document = Jsoup.parse(raw)
            val cleaned = document.body().wholeText().ifBlank { raw }
            return JSONObject(cleaned)
        }
    }

    private fun parseManga(item: JSONObject): MangaItem {
        val data = item.optJSONObject("data") ?: JSONObject()
        val genresArray = data.optJSONArray("genres") ?: JSONArray()
        val genres = buildList {
            for (i in 0 until genresArray.length()) {
                val name = genresArray.optJSONObject(i)?.optJSONObject("data")?.optString("name").orEmpty()
                if (name.isNotBlank()) add(name)
            }
        }
        val totalChapters = cleanText(data.opt("totalChapters")?.toString().orEmpty()).ifBlank { "-" }
        return MangaItem(
            id = item.optLong("id"),
            slug = data.optString("slug"),
            title = cleanText(data.optString("title")).ifBlank { "Tanpa Judul" },
            coverImage = data.optString("coverImage"),
            latestChapter = if (totalChapters != "-") "Chapter $totalChapters" else "",
            synopsis = cleanText(data.optString("synopsis")).ifBlank { "Tidak ada deskripsi." },
            author = cleanText(data.optString("author")).ifBlank { "-" },
            format = cleanText(data.optString("format")).ifBlank { "-" },
            rating = data.opt("rating")?.toString() ?: "-",
            status = data.optString("status").ifBlank { "-" },
            nativeTitle = data.optString("nativeTitle").ifBlank { "-" },
            releaseDate = data.optString("releaseDate").ifBlank { "-" },
            totalChapters = totalChapters,
            genres = genres
        )
    }

    private fun encryptText(value: String): String {
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, favoriteSecretKey(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val output = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, output, 0, iv.size)
        System.arraycopy(encrypted, 0, output, iv.size, encrypted.size)
        return Base64.encodeToString(output, Base64.NO_WRAP)
    }

    private fun decryptText(value: String): String {
        val input = Base64.decode(value, Base64.NO_WRAP)
        val iv = input.copyOfRange(0, 12)
        val encrypted = input.copyOfRange(12, input.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, favoriteSecretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun favoriteSecretKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest("co.komikcast.app.favorite.aes".toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    private fun defaultUserAgent(): String {
        return try {
            WebSettings.getDefaultUserAgent(context).replace("; wv", "")
        } catch (_: Exception) {
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"
        }
    }

    private fun cleanText(value: String): String {
        val text = value.trim()
        return if (text.equals("null", ignoreCase = true)) "" else text
    }

    private fun formatReleaseDate(value: String): String {
        val text = cleanText(value)
        if (text.isBlank()) return ""
        return text.substringBefore("T").ifBlank { text }
    }

    private fun normalizeChapterIndex(value: String): String {
        return parseChapterIndex(value)
    }

    private suspend fun <T> awaitFuture(block: () -> T): T {
        return withContext(dispatcher) {
            CompletableFuture.supplyAsync(Supplier { block() }, executor).get()
        }
    }
}

fun displayChapterIndex(index: String): String {
    return index.removeSuffix(".0")
}

fun numericChapterIndex(index: String): Double {
    return index.toDoubleOrNull() ?: index.removeSuffix(".0").toDoubleOrNull() ?: 0.0
}

fun parseChapterIndex(value: Any?): String {
    return when (value) {
        is Number -> {
            val doubleValue = value.toDouble()
            if (doubleValue % 1.0 == 0.0) "${doubleValue.toLong()}.0" else doubleValue.toString()
        }
        is String -> {
            val doubleValue = value.toDoubleOrNull()
            if (doubleValue != null && doubleValue % 1.0 == 0.0) "${doubleValue.toLong()}.0" else value
        }
        else -> "0.0"
    }
}
