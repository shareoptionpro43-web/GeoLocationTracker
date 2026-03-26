package com.example.geolocationtracker.ui

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.geolocationtracker.databinding.ActivityGeofenceBinding
import com.example.geolocationtracker.utils.GeofenceHelper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.math.RoundingMode
import java.util.UUID

class GeofenceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGeofenceBinding
    private val vm: LocationViewModel by viewModels()
    private lateinit var map: MapView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = "GeoLocationTracker/1.0 (Android)"

        binding = ActivityGeofenceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Geofence Zones (OSM)"

        map = binding.osmGeofenceMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(14.0)

        val myLoc = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLoc.enableMyLocation()
        map.overlays.add(myLoc)

        // Long press to add geofence
        map.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onLongPress(e: android.view.MotionEvent, mapView: MapView): Boolean {
                val proj = mapView.projection
                val pt = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                showAddDialog(pt)
                return true
            }
        })

        // Draw existing zones
        GeofenceHelper.getZones().forEach { drawZone(it) }

        vm.current.value?.let {
            map.controller.animateTo(GeoPoint(it.latitude, it.longitude))
        }

        binding.btnClearZones.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Remove all zones?")
                .setPositiveButton("Remove") { _, _ ->
                    GeofenceHelper.removeAll(this)
                    map.overlays.removeAll { it is Polygon || it is Marker }
                    map.invalidate()
                    updateCount()
                    Toast.makeText(this, "All zones removed", Toast.LENGTH_SHORT).show()
                }.setNegativeButton("Cancel", null).show()
        }
        updateCount()
    }

    private fun showAddDialog(pt: GeoPoint) {
        val nameInput = EditText(this).apply { hint = "Zone name (e.g. Home)" }
        val radiusInput = EditText(this).apply {
            hint = "Radius metres"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText("200")
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 16, 48, 0)
            addView(TextView(context).apply { text = "Zone name:" })
            addView(nameInput)
            addView(TextView(context).apply { text = "Radius (m):"; setPadding(0, 16, 0, 0) })
            addView(radiusInput)
        }
        val coordTxt = "${pt.latitude.toBigDecimal().setScale(5, RoundingMode.HALF_UP)}, " +
                "${pt.longitude.toBigDecimal().setScale(5, RoundingMode.HALF_UP)}"

        AlertDialog.Builder(this).setTitle("Add Geofence Zone")
            .setMessage("📍 $coordTxt").setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().ifEmpty { "Zone ${GeofenceHelper.getZones().size + 1}" }
                val radius = radiusInput.text.toString().toFloatOrNull() ?: 200f
                val zone = GeofenceHelper.Zone(UUID.randomUUID().toString(), name, pt.latitude, pt.longitude, radius)
                if (GeofenceHelper.add(this, zone)) {
                    drawZone(zone); updateCount()
                    Toast.makeText(this, "Zone '$name' added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Background location permission needed", Toast.LENGTH_LONG).show()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun drawZone(zone: GeofenceHelper.Zone) {
        val circle = Polygon(map).apply {
            points = Polygon.pointsAsCircle(GeoPoint(zone.lat, zone.lng), zone.radius.toDouble())
            fillPaint.color = Color.argb(40, 33, 150, 243)
            outlinePaint.color = Color.parseColor("#2196F3")
            outlinePaint.strokeWidth = 3f
            title = zone.name
            snippet = "Radius: ${zone.radius.toInt()}m"
        }
        map.overlays.add(circle)
        val marker = Marker(map).apply {
            position = GeoPoint(zone.lat, zone.lng)
            title = zone.name
            snippet = "Radius: ${zone.radius.toInt()}m"
        }
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun updateCount() {
        val n = GeofenceHelper.getZones().size
        binding.tvZoneCount.text = "$n active zone${if (n != 1) "s" else ""}"
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
