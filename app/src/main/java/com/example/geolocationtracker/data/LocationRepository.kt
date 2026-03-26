package com.example.geolocationtracker.data

import android.content.Context
import androidx.lifecycle.LiveData
import java.util.UUID

class LocationRepository(ctx: Context) {
    private val dao = AppDatabase.get(ctx).locationDao()

    val allLocations: LiveData<List<LocationEntity>> = dao.allLocations()
    fun recent(n: Int = 200): LiveData<List<LocationEntity>> = dao.recent(n)

    suspend fun insert(loc: LocationEntity) = dao.insert(loc)
    suspend fun latest() = dao.latest()
    suspend fun count() = dao.count()
    suspend fun sessions() = dao.sessions()
    suspend fun bySession(sid: String) = dao.bySession(sid)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun delete(loc: LocationEntity) = dao.delete(loc)
    suspend fun deleteOlderThan(ts: Long) = dao.deleteOlderThan(ts)

    fun newSessionId(): String = UUID.randomUUID().toString().take(8)
}
