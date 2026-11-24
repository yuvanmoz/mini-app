package com.mini.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mini.app.data.local.entities.AuthorEntity
import com.mini.app.data.local.entities.MediaItemEntity
import com.mini.app.data.local.entities.PostEntity
import com.mini.app.data.local.entities.PostTagCrossRef
import com.mini.app.data.local.entities.TagEntity

@Database(
    entities = [
        PostEntity::class,
        AuthorEntity::class,
        MediaItemEntity::class,
        TagEntity::class,
        PostTagCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
