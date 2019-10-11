package com.andb.apps.trails.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.andb.apps.trails.converters.IDListConverter
import com.andb.apps.trails.converters.SkiAreaDetailsConverter
import com.andb.apps.trails.converters.ThumbnailListConverter
import com.andb.apps.trails.objects.*
import dev.matrix.roomigrant.GenerateRoomMigrations

@Database(entities = [SkiMap::class, SkiArea::class, SkiRegion::class], version = 5, exportSchema = true)
@TypeConverters(value = [ThumbnailListConverter::class, IDListConverter::class, SkiAreaDetailsConverter::class])
@GenerateRoomMigrations
abstract class Database : RoomDatabase() {
    abstract fun mapsDao(): MapsDao
    abstract fun areasDao(): AreasDao
    abstract fun regionsDao(): RegionsDao

    companion object {
        lateinit var db: com.andb.apps.trails.database.Database

        fun setDB(ctxt: Context) {
            db = Room.databaseBuilder(ctxt, com.andb.apps.trails.database.Database::class.java, "TrailsDatabase")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
        }
    }

}

fun db() = com.andb.apps.trails.database.Database.db
fun mapsDao() = db().mapsDao()
fun areasDao() = db().areasDao()
fun regionsDao() = db().regionsDao()



val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(addBaseInt("liftCount"))
        database.execSQL(addBaseInt("runCount"))
        database.execSQL(addBaseInt("openingYear"))
        database.execSQL(addBaseText("website"))

        database.execSQL("ALTER TABLE Map RENAME TO SkiMap")
        database.execSQL(addMapInt("liftCount"))
        database.execSQL(addMapInt("runCount"))
        database.execSQL(addMapInt("openingYear"))
        database.execSQL(addMapText("website"))
    }

    fun addBaseInt(columnName: String): String {
        return "ALTER TABLE SkiArea ADD COLUMN $columnName INTEGER DEFAULT -1 NOT NULL "
    }

    fun addBaseText(columnName: String): String {
        return "ALTER TABLE SkiArea ADD COLUMN $columnName TEXT DEFAULT '' NOT NULL "
    }

    fun addMapInt(columnName: String): String {
        return "ALTER TABLE SkiMap ADD COLUMN $columnName INTEGER DEFAULT -1 NOT NULL "
    }

    fun addMapText(columnName: String): String {
        return "ALTER TABLE SkiMap ADD COLUMN $columnName TEXT DEFAULT '' NOT NULL "
    }
}