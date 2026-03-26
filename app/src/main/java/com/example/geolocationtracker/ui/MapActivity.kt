package com.example.geolocationtracker.ui

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.geolocationtracker.databinding.ActivityMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private val vm: LocationViewModel by viewModels()
    private lateinit var map: MapView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        // OSMDroid config — must be done before setContentView
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = "GeoLocationTracker/1.0 (Android)"

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Live Map (OpenStreetMap)"

        map = binding.osmMapView
        map.setTileSource(TileSourceFactory.MAPNIK)   // Standard OSM tiles
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        // Show my location dot
        val myLocation = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocation.enableMyLocation()
        map.overlays.add(myLocation)

        vm.recentLocations.observe(this) { locs ->
            drawRoute(locs.reversed())
        }
    }

    private fun drawRoute(locs: List<com.example.geolocationtracker.data.LocationEntity>) {
        // Remove previous route overlays (keep MyLocation overlay)
        map.overlays.removeAll { it is Polyline || it is Marker }

        if (locs.isEmpty()) return

        val points = locs.map { GeoPoint(it.latitude, it.longitude) }

        // Draw polyline route
        if (points.size > 1) {
            val polyline = Polyline(map).apply {
                setPoints(points)
                outlinePaint.color = Color.parseColor("#2196F3")
                outlinePaint.strokeWidth = 8f
            }
            map.overlays.add(polyline)
        }

        // Start marker (green)
        val startMarker = Marker(map).apply {
            position = points.first()
            title = "Start"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(startMarker)

        // Current / latest marker (shown with info)
        val current = locs.last()
        val currentMarker = Marker(map).apply {
            position = points.last()
            title = "Current Location"
            snippet = "${current.formattedCoords()}\n${current.formattedTime()}"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(currentMarker)
        currentMarker.showInfoWindow()

        // Pan + zoom to current location
        map.controller.animateTo(points.last())

        binding.tvMapInfo.text = "${locs.size} points tracked  •  OpenStreetMap"
        map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
