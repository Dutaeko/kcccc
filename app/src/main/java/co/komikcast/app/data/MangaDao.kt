package co.komikcast.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM favorite_manga ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteMangaEntity>>

    @Query("SELECT * FROM favorite_manga WHERE slug = :slug LIMIT 1")
    fun observeFavorite(slug: String): Flow<FavoriteMangaEntity?>

    @Query("SELECT * FROM favorite_manga WHERE slug = :slug LIMIT 1")
    suspend fun getFavorite(slug: String): FavoriteMangaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entity: FavoriteMangaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorites(entities: List<FavoriteMangaEntity>)

    @Query("SELECT * FROM favorite_manga ORDER BY addedAt DESC")
    suspend fun getFavorites(): List<FavoriteMangaEntity>

    @Delete
    suspend fun deleteFavorite(entity: FavoriteMangaEntity)

    @Query("SELECT * FROM reading_history WHERE updatedAt IN (SELECT MAX(updatedAt) FROM reading_history GROUP BY seriesSlug) ORDER BY updatedAt DESC")
    fun observeLatestHistories(): Flow<List<ReadingHistoryEntity>>

    @Query("SELECT * FROM reading_history WHERE seriesSlug = :slug ORDER BY updatedAt DESC")
    fun observeHistoriesForManga(slug: String): Flow<List<ReadingHistoryEntity>>

    @Query("SELECT * FROM reading_history WHERE seriesSlug = :slug ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLastHistory(slug: String): ReadingHistoryEntity?

    @Query("SELECT * FROM reading_history WHERE seriesSlug = :slug AND chapterIndex = :chapterIndex LIMIT 1")
    suspend fun getHistory(slug: String, chapterIndex: String): ReadingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: ReadingHistoryEntity)

    @Query("DELETE FROM reading_history")
    suspend fun clearHistories()

    @Query("DELETE FROM reading_history WHERE seriesSlug IN (:slugs)")
    suspend fun deleteHistoriesBySeries(slugs: List<String>)
}
