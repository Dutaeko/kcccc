package co.komikcast.app.data

import androidx.room.Entity

@Entity(tableName = "favorite_manga", primaryKeys = ["slug"])
data class FavoriteMangaEntity(
    val slug: String,
    val title: String,
    val coverImage: String,
    val latestChapter: String,
    val addedAt: Long
)

@Entity(tableName = "reading_history", primaryKeys = ["seriesSlug", "chapterIndex"])
data class ReadingHistoryEntity(
    val seriesSlug: String,
    val seriesTitle: String,
    val coverImage: String,
    val chapterIndex: String,
    val chapterTitle: String,
    val progress: Int,
    val lastImageIndex: Int,
    val totalImages: Int,
    val updatedAt: Long
)

fun FavoriteMangaEntity.toMangaItem(): MangaItem {
    return MangaItem(
        id = 0L,
        slug = slug,
        title = title,
        coverImage = coverImage,
        latestChapter = latestChapter
    )
}
