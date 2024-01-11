package com.nexus.farmap.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nexus.farmap.data.model.NeighboursConverter
import com.nexus.farmap.data.model.TreeNodeDto

@Database(
    entities = [TreeNodeDto::class], version = 1
)
@TypeConverters(NeighboursConverter::class)
abstract class GraphDatabase : RoomDatabase() {

    abstract val graphDao: GraphDao

    companion object {
        const val DATABASE_NAME = "gdb"
    }
}