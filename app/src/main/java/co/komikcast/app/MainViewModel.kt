package co.komikcast.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.komikcast.app.data.ChapterItem
import co.komikcast.app.data.DetailUiState
import co.komikcast.app.data.FavoriteMangaEntity
import co.komikcast.app.data.GenreItem
import co.komikcast.app.data.KomikcastDatabase
import co.komikcast.app.data.KomikcastRepository
import co.komikcast.app.data.MangaItem
import co.komikcast.app.data.PageUiState
import co.komikcast.app.data.ReaderChapter
import co.komikcast.app.data.ReaderUiState
import co.komikcast.app.data.ReadingHistoryEntity
import co.komikcast.app.data.displayChapterIndex
import co.komikcast.app.data.numericChapterIndex
import co.komikcast.app.data.parseChapterIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = KomikcastDatabase.get(application)
    private val repository = KomikcastRepository(application, database.mangaDao())

    private val _home = MutableStateFlow(PageUiState())
    val home: StateFlow<PageUiState> = _home.asStateFlow()

    private val _popular = MutableStateFlow(PageUiState())
    val popular: StateFlow<PageUiState> = _popular.asStateFlow()

    private val _search = MutableStateFlow(PageUiState())
    val search: StateFlow<PageUiState> = _search.asStateFlow()

    private val _genreResult = MutableStateFlow(PageUiState())
    val genreResult: StateFlow<PageUiState> = _genreResult.asStateFlow()

    private val _genreResultTitle = MutableStateFlow("")
    val genreResultTitle: StateFlow<String> = _genreResultTitle.asStateFlow()

    private val _detail = MutableStateFlow(DetailUiState())
    val detail: StateFlow<DetailUiState> = _detail.asStateFlow()

    private val _reader = MutableStateFlow(ReaderUiState())
    val reader: StateFlow<ReaderUiState> = _reader.asStateFlow()

    private val _genres = MutableStateFlow<List<GenreItem>>(emptyList())
    val genres: StateFlow<List<GenreItem>> = _genres.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val favorites: StateFlow<List<FavoriteMangaEntity>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val histories: StateFlow<List<ReadingHistoryEntity>> = repository.observeLatestHistories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshHome()
        refreshPopular()
        loadGenres()
    }

    fun observeFavorite(slug: String): Flow<Boolean> = repository.observeFavorite(slug).map { it != null }

    fun observeHistoriesForManga(slug: String): Flow<Map<String, ReadingHistoryEntity>> {
        return repository.observeHistoriesForManga(slug).map { list -> list.associateBy { it.chapterIndex } }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setGenre(value: String?) {
        _selectedGenre.value = value
        refreshSearch()
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun refreshHome() {
        loadHome(refresh = true)
    }

    fun refreshPopular() {
        loadPopular(refresh = true)
    }

    fun refreshSearch() {
        loadSearch(refresh = true)
    }

    fun openGenreResult(genre: String) {
        _genreResultTitle.value = genre
        loadGenreResult(refresh = true)
    }

    fun loadMoreHome() {
        loadHome(refresh = false)
    }

    fun loadMorePopular() {
        loadPopular(refresh = false)
    }

    fun loadMoreSearch() {
        loadSearch(refresh = false)
    }

    fun loadMoreGenreResult() {
        loadGenreResult(refresh = false)
    }

    fun openDetail(manga: MangaItem) {
        openDetail(manga.slug)
    }

    fun openDetail(slug: String, refresh: Boolean = true) {
        viewModelScope.launch {
            val current = _detail.value
            val sameManga = current.detail?.manga?.slug == slug
            _detail.value = if (sameManga && refresh) {
                current.copy(isLoading = false, isRefreshing = true, error = null)
            } else {
                DetailUiState(isLoading = true)
            }
            runCatching {
                val detail = repository.detail(slug)
                val chapters = repository.chapters(slug)
                detail to chapters
            }.onSuccess { result ->
                _detail.value = DetailUiState(detail = result.first, chapters = result.second)
            }.onFailure { error ->
                _detail.value = DetailUiState(error = error.message ?: "Gagal mengambil detail")
            }
        }
    }

    fun refreshDetail() {
        val slug = _detail.value.detail?.manga?.slug ?: return
        openDetail(slug, refresh = true)
    }

    fun toggleFavorite(manga: MangaItem) {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(manga) }
                .onFailure { _message.value = it.message ?: "Favorite gagal diproses" }
        }
    }

    fun startReading(onReady: () -> Unit) {
        val current = _detail.value
        val manga = current.detail?.manga ?: return
        viewModelScope.launch {
            val chapter = repository.startChapterFor(manga.slug, current.chapters) ?: return@launch
            openReaderInternal(manga.slug, manga.title, manga.coverImage, chapter.index, onReady)
        }
    }

    fun openReader(slug: String, title: String, coverImage: String, chapterIndex: String, onReady: () -> Unit) {
        viewModelScope.launch {
            openReaderInternal(slug, title, coverImage, chapterIndex, onReady)
        }
    }

    fun openLatestReader(manga: MangaItem, onReady: () -> Unit) {
        val rawIndex = manga.latestChapter.substringAfter("Chapter", "").trim().ifBlank { manga.totalChapters }
        val chapterIndex = parseChapterIndex(rawIndex.ifBlank { "0.0" })
        viewModelScope.launch {
            openReaderInternal(manga.slug, manga.title, manga.coverImage, chapterIndex, onReady)
        }
    }

    fun refreshReader() {
        val current = _reader.value.reader ?: return
        viewModelScope.launch {
            openReaderInternal(current.seriesSlug, current.seriesTitle, current.coverImage, current.chapterIndex) {}
        }
    }

    fun readerPrev() {
        val current = _reader.value.reader ?: return
        val target = findAdjacentChapter(current, -1) ?: return
        viewModelScope.launch {
            openReaderInternal(current.seriesSlug, current.seriesTitle, current.coverImage, target.index) {}
        }
    }

    fun readerNext() {
        val current = _reader.value.reader ?: return
        val target = findAdjacentChapter(current, 1) ?: return
        viewModelScope.launch {
            openReaderInternal(current.seriesSlug, current.seriesTitle, current.coverImage, target.index) {}
        }
    }

    fun markReaderProgress(index: Int) {
        val current = _reader.value.reader ?: return
        viewModelScope.launch {
            repository.markHistory(current, index)
        }
    }

    fun saveCover(manga: MangaItem) {
        viewModelScope.launch {
            runCatching { repository.saveCover(manga) }
                .onSuccess { saved -> _message.value = if (saved) "Cover berhasil disimpan" else "Cover gagal disimpan" }
                .onFailure { _message.value = it.message ?: "Cover gagal disimpan" }
        }
    }

    fun exportFavorites(onReady: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.exportFavoritesEncrypted() }
                .onSuccess { onReady(it) }
                .onFailure { _message.value = it.message ?: "Export favorite gagal" }
        }
    }

    fun importFavorites(value: String) {
        viewModelScope.launch {
            runCatching { repository.importFavoritesEncrypted(value) }
                .onSuccess { _message.value = "Import favorite berhasil" }
                .onFailure { _message.value = it.message ?: "Import favorite gagal" }
        }
    }

    fun clearHistories() {
        viewModelScope.launch {
            runCatching { repository.clearHistories() }
                .onFailure { _message.value = it.message ?: "History gagal dihapus" }
        }
    }

    fun deleteHistories(slugs: Set<String>) {
        viewModelScope.launch {
            runCatching { repository.deleteHistories(slugs.toList()) }
                .onFailure { _message.value = it.message ?: "History gagal dihapus" }
        }
    }

    private fun loadGenres() {
        viewModelScope.launch {
            runCatching { repository.genres() }
                .onSuccess { _genres.value = it }
        }
    }

    private fun loadHome(refresh: Boolean) {
        val current = _home.value
        if (current.isLoading || current.isRefreshing) return
        if (!refresh && current.page >= current.lastPage) return
        val nextPage = if (refresh) 1 else current.page + 1
        val loadingState = current.copy(isLoading = !refresh, isRefreshing = refresh, error = null)
        _home.value = loadingState
        viewModelScope.launch {
            runCatching { repository.latest(nextPage) }
                .onSuccess { page ->
                    _home.value = PageUiState(
                        items = (if (refresh) page.items else current.items + page.items).distinctBy { it.slug },
                        page = page.page,
                        lastPage = page.lastPage
                    )
                }
                .onFailure { _home.value = current.copy(isLoading = false, isRefreshing = false, error = it.message ?: "Gagal mengambil Home") }
        }
    }

    private fun loadPopular(refresh: Boolean) {
        val current = _popular.value
        if (current.isLoading || current.isRefreshing) return
        if (!refresh && current.page >= current.lastPage) return
        val nextPage = if (refresh) 1 else current.page + 1
        val loadingState = current.copy(isLoading = !refresh, isRefreshing = refresh, error = null)
        _popular.value = loadingState
        viewModelScope.launch {
            runCatching { repository.popular(nextPage) }
                .onSuccess { page ->
                    _popular.value = PageUiState(
                        items = (if (refresh) page.items else current.items + page.items).distinctBy { it.slug },
                        page = page.page,
                        lastPage = page.lastPage
                    )
                }
                .onFailure { _popular.value = current.copy(isLoading = false, isRefreshing = false, error = it.message ?: "Gagal mengambil Populer") }
        }
    }

    private fun loadSearch(refresh: Boolean) {
        val current = _search.value
        val query = _query.value
        val genre = _selectedGenre.value
        if (current.isLoading || current.isRefreshing) return
        if (!refresh && current.page >= current.lastPage) return
        val nextPage = if (refresh) 1 else current.page + 1
        val loadingState = current.copy(isLoading = !refresh, isRefreshing = refresh, error = null)
        _search.value = loadingState
        viewModelScope.launch {
            runCatching { repository.search(nextPage, query, genre) }
                .onSuccess { page ->
                    _search.value = PageUiState(
                        items = (if (refresh) page.items else current.items + page.items).distinctBy { it.slug },
                        page = page.page,
                        lastPage = page.lastPage
                    )
                }
                .onFailure { _search.value = current.copy(isLoading = false, isRefreshing = false, error = it.message ?: "Gagal mencari manga") }
        }
    }

    private fun loadGenreResult(refresh: Boolean) {
        val current = _genreResult.value
        val genre = _genreResultTitle.value
        if (genre.isBlank()) return
        if (current.isLoading || current.isRefreshing) return
        if (!refresh && current.page >= current.lastPage) return
        val nextPage = if (refresh) 1 else current.page + 1
        _genreResult.value = current.copy(isLoading = !refresh, isRefreshing = refresh, error = null)
        viewModelScope.launch {
            runCatching { repository.genre(nextPage, genre) }
                .onSuccess { page ->
                    _genreResult.value = PageUiState(
                        items = (if (refresh) page.items else current.items + page.items).distinctBy { it.slug },
                        page = page.page,
                        lastPage = page.lastPage
                    )
                }
                .onFailure { _genreResult.value = current.copy(isLoading = false, isRefreshing = false, error = it.message ?: "Gagal mengambil genre") }
        }
    }

    private suspend fun openReaderInternal(slug: String, title: String, coverImage: String, chapterIndex: String, onReady: () -> Unit) {
        _reader.value = ReaderUiState(isLoading = true)
        runCatching { repository.chapter(slug, chapterIndex, title, coverImage) }
            .onSuccess { data ->
                _reader.value = ReaderUiState(reader = data)
                repository.markHistory(data, 0)
                onReady()
            }
            .onFailure { error ->
                _reader.value = ReaderUiState(error = error.message ?: "Gagal membuka reader")
            }
    }

    private fun findAdjacentChapter(reader: ReaderChapter, offset: Int): ChapterItem? {
        val chapters = reader.chapters.sortedBy { numericChapterIndex(it.index) }
        val currentIndex = chapters.indexOfFirst { numericChapterIndex(it.index) == numericChapterIndex(reader.chapterIndex) }
        if (currentIndex < 0) return null
        return chapters.getOrNull(currentIndex + offset)
    }

    fun chapterLabel(index: String): String = "Chapter ${displayChapterIndex(index)}"
}
