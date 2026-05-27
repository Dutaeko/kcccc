package co.komikcast.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteMangaEntity::class, ReadingHistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KomikcastDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao

    companion object {
        @Volatile
        private var instance: KomikcastDatabase? = null

        fun get(context: Context): KomikcastDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KomikcastDatabase::class.java,
                    "komikcast.db"
                ).build().also { instance = it }
            }
        }
    }
}
