package com.example.geolocationtracker.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.geolocationtracker.R
import com.example.geolocationtracker.service.LocationTrackingService
import com.example.geolocationtracker.ui.MainActivity
import com.google.android.gms.location.*

object GeofenceHelper {

    data class Zone(val id: String, val name: String, val lat: Double, val lng: Double, val radius: Float)

    private val zones = mutableListOf<Zone>()

    fun getZones(): List<Zone> = zones.toList()

    fun add(context: Context, zone: Zone): Boolean {
        return try {
            val client = LocationServices.getGeofencingClient(context)
            val geofence = Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.lat, zone.lng, zone.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
            val req = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build()
            client.addGeofences(req, pendingIntent(context))
                .addOnSuccessListener { zones.add(zone) }
                .addOnFailureListener { Log.e("Geofence", "Add failed: ${it.message}") }
            true
        } catch (e: SecurityException) {
            Log.e("Geofence", "No permission", e); false
        }
    }

    fun removeAll(context: Context) {
        LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent(context))
        zones.clear()
    }

    private fun pendingIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 0, Intent(ctx, GeofenceBroadcastReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
}

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val transition = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered"
            Geofence.GEOFENCE_TRANSITION_EXIT  -> "Exited"
            else -> return
        }
        event.triggeringGeofences?.forEach { gf ->
            val zone = GeofenceHelper.getZones().find { it.id == gf.requestId }
            notify(ctx, zone?.name ?: gf.requestId, transition, gf.requestId.hashCode())
        }
    }

    private fun notify(ctx: Context, name: String, transition: String, id: Int) {
        val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(ctx, LocationTrackingService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location_pin)
            .setContentTitle("📍 Geofence Alert")
            .setContentText("$transition: $name")
            .setContentIntent(pi).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notif)
    }
}
