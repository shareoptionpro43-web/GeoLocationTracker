package com.example.geolocationtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val address: String = "",
    val sessionId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedTime(): String =
        SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    fun formattedDate(): String =
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))

    fun formattedCoords(): String = "%.6f, %.6f".format(latitude, longitude)

    fun speedKmh(): Float = speed * 3.6f

    fun formattedSpeed(): String = "${"%.1f".format(speedKmh())} km/h"

    fun formattedAltitude(): String = "${"%.1f".format(altitude)} m"

    fun formattedAccuracy(): String = "±${"%.0f".format(accuracy)} m"
}
