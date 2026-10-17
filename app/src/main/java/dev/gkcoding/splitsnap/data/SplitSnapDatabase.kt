package dev.gkcoding.splitsnap.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BillEntity::class, PersonEntity::class, ItemEntity::class, AssignmentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SplitSnapDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao

    companion object {
        @Volatile private var instance: SplitSnapDatabase? = null

        fun get(context: Context): SplitSnapDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SplitSnapDatabase::class.java,
                    "splitsnap.db",
                ).build().also { instance = it }
            }
    }
}
