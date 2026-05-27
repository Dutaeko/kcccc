package co.komikcast.app.data

data class MangaItem(
    val id: Long,
    val slug: String,
    val title: String,
    val coverImage: String,
    val latestChapter: String,
    val synopsis: String = "",
    val author: String = "-",
    val format: String = "-",
    val rating: String = "-",
    val status: String = "-",
    val nativeTitle: String = "-",
    val releaseDate: String = "-",
    val totalChapters: String = "-",
    val genres: List<String> = emptyList()
)

data class MangaPage(
    val items: List<MangaItem>,
    val page: Int,
    val lastPage: Int
)

data class ChapterItem(
    val id: Long,
    val index: String,
    val title: String,
    val releaseDate: String = ""
)

data class MangaDetail(
    val manga: MangaItem,
    val information: List<Pair<String, String>>
)

data class ReaderChapter(
    val seriesSlug: String,
    val seriesTitle: String,
    val coverImage: String,
    val chapterIndex: String,
    val chapterTitle: String,
    val images: List<String>,
    val chapters: List<ChapterItem>
)

data class GenreItem(
    val id: Int,
    val name: String
)

enum class MangaFeedType {
    Latest,
    Popular,
    Search
}

data class PageUiState(
    val items: List<MangaItem> = emptyList(),
    val page: Int = 0,
    val lastPage: Int = 1,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

data class DetailUiState(
    val detail: MangaDetail? = null,
    val chapters: List<ChapterItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

data class ReaderUiState(
    val reader: ReaderChapter? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
