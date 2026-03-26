package com.example.geolocationtracker.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.geolocationtracker.data.LocationEntity
import com.example.geolocationtracker.data.LocationRepository
import kotlinx.coroutines.launch
import kotlin.math.*

class LocationViewModel(app: Application) : AndroidViewModel(app) {
    val repo = LocationRepository(app)

    val recentLocations: LiveData<List<LocationEntity>> = repo.recent(300)
    val allLocations: LiveData<List<LocationEntity>> = repo.allLocations

    private val _current = MutableLiveData<LocationEntity?>()
    val current: LiveData<LocationEntity?> = _current

    private val _isTracking = MutableLiveData(false)
    val isTracking: LiveData<Boolean> = _isTracking

    // Stats derived from recent locations
    val stats: LiveData<TrackStats> = recentLocations.map { locs ->
        computeStats(locs)
    }

    init {
        recentLocations.observeForever { if (it.isNotEmpty()) _current.postValue(it.first()) }
    }

    fun setTracking(v: Boolean) { _isTracking.value = v }

    fun clearHistory() = viewModelScope.launch { repo.deleteAll() }

    fun delete(loc: LocationEntity) = viewModelScope.launch { repo.delete(loc) }

    private fun computeStats(locs: List<LocationEntity>): TrackStats {
        if (locs.isEmpty()) return TrackStats()
        val speeds = locs.map { it.speedKmh() }
        var dist = 0.0
        val sorted = locs.sortedBy { it.timestamp }
        for (i in 1 until sorted.size) {
            dist += haversine(sorted[i-1].latitude, sorted[i-1].longitude,
                sorted[i].latitude, sorted[i].longitude)
        }
        val duration = if (sorted.size > 1) sorted.last().timestamp - sorted.first().timestamp else 0L
        return TrackStats(
            pointCount = locs.size,
            maxSpeedKmh = speeds.max(),
            avgSpeedKmh = speeds.average().toFloat(),
            totalDistanceM = dist,
            durationMs = duration
        )
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon/2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1-a))
    }
}

data class TrackStats(
    val pointCount: Int = 0,
    val maxSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val totalDistanceM: Double = 0.0,
    val durationMs: Long = 0L
) {
    fun formattedDistance() = if (totalDistanceM >= 1000)
        "${"%.2f".format(totalDistanceM/1000)} km" else "${"%.0f".format(totalDistanceM)} m"

    fun formattedDuration(): String {
        val s = durationMs / 1000
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m ${sec}s" else "${sec}s"
    }
}
