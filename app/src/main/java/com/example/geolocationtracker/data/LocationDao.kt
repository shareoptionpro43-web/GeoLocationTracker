package com.example.geolocationtracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity): Long

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun allLocations(): LiveData<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT :n")
    fun recent(n: Int = 200): LiveData<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE sessionId = :sid ORDER BY timestamp ASC")
    suspend fun bySession(sid: String): List<LocationEntity>

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): LocationEntity?

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun count(): Int

    @Query("SELECT DISTINCT sessionId FROM locations ORDER BY timestamp DESC")
    suspend fun sessions(): List<String>

    @Query("DELETE FROM locations")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(location: LocationEntity)

    @Query("DELETE FROM locations WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
