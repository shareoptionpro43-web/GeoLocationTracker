package com.example.geolocationtracker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.geolocationtracker.R
import com.example.geolocationtracker.data.LocationEntity
import com.example.geolocationtracker.data.LocationRepository
import com.example.geolocationtracker.ui.MainActivity
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class LocationTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "geo_tracker_channel"
        const val NOTIF_ID = 1001
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_INTERVAL_MS = "interval_ms"
        const val EXTRA_SESSION = "session_id"
        var isRunning = false
        var currentSessionId = ""
    }

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var repo: LocationRepository
    private lateinit var callback: LocationCallback
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient()
    private var intervalMs = 5_000L

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        repo = LocationRepository(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 5_000L)
                currentSessionId = intent.getStringExtra(EXTRA_SESSION) ?: repo.newSessionId()
                startTracking()
            }
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        isRunning = true
        startForeground(NOTIF_ID, buildNotif("Tracking active…"))
        callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                res.lastLocation?.let { loc ->
                    scope.launch {
                        val addr = reverseGeocodeNominatim(loc.latitude, loc.longitude)
                        repo.insert(
                            LocationEntity(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                altitude = loc.altitude,
                                accuracy = loc.accuracy,
                                speed = loc.speed,
                                bearing = loc.bearing,
                                address = addr,
                                sessionId = currentSessionId
                            )
                        )
                        updateNotif(if (addr.isNotEmpty()) addr.take(55) else
                            "${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}")
                    }
                }
            }
        }
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()
        try {
            fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("GeoService", "Permission denied", e); stopSelf()
        }
    }

    private fun stopTracking() {
        isRunning = false
        if (::callback.isInitialized) fused.removeLocationUpdates(callback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Nominatim reverse geocoding — OpenStreetMap's free geocoder, no API key needed.
     * Policy: provide a meaningful User-Agent, max 1 req/sec (GPS intervals are already ≥1s).
     */
    private fun reverseGeocodeNominatim(lat: Double, lon: Double): String {
        return try {
            val url = "https://nominatim.openstreetmap.org/reverse" +
                    "?format=jsonv2&lat=$lat&lon=$lon&zoom=18&addressdetails=0"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "GeoLocationTracker/1.0 (Android)")
                .header("Accept-Language", "en")
                .build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return ""
            JSONObject(body).optString("display_name", "")
        } catch (e: Exception) {
            Log.w("Geocoder", "Nominatim failed: ${e.message}")
            ""
        }
    }

    private fun buildNotif(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getService(this, 1,
            Intent(this, LocationTrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location_pin)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pi)
            .addAction(0, "Stop", stopPi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotif(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotif("📍 $text"))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Location Tracking",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows while location tracking is active"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
        if (::callback.isInitialized) fused.removeLocationUpdates(callback)
    }
}
