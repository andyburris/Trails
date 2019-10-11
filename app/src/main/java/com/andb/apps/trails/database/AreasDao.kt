package com.andb.apps.trails.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.andb.apps.trails.objects.SkiArea


@Dao
interface AreasDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertArea(area: SkiArea)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMultipleAreas(areas: List<SkiArea>)

    @Update
    fun updateArea(area: SkiArea)

    @Delete
    fun deleteArea(area: SkiArea)

    @Query("SELECT * FROM SkiArea WHERE areaID = :id")
    fun getAreaByID(id: Int): SkiArea?

    @Query("SELECT * FROM SkiArea")
    fun getAll(): LiveData<List<SkiArea>>

    @Query("SELECT * FROM SkiArea")
    fun getAllStatic(): List<SkiArea>

    @Query("SELECT * FROM SkiArea WHERE favorite = 1")
    fun getFavorites(): LiveData<List<SkiArea>>

    @Query("SELECT * FROM SkiArea WHERE areaName LIKE :text")
    fun search(text: String): List<SkiArea>
}