package com.techv1.xplay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.techv1.xplay.data.local.dao.VideoDao
import com.techv1.xplay.data.local.entity.FavoriteEntity
import com.techv1.xplay.data.local.entity.HistoryEntity

@Database(
    entities = [HistoryEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class XPlayDatabase : RoomDatabase() {
    abstract val videoDao: VideoDao

    companion object {
        const val DATABASE_NAME = "xplay_db"
    }
}
