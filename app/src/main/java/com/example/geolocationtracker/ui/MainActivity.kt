package com.example.geolocationtracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.geolocationtracker.R
import com.example.geolocationtracker.databinding.ActivityMainBinding
import com.example.geolocationtracker.service.LocationTrackingService
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: LocationViewModel by viewModels()
    private var intervalMs = 5_000L

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            doStartTracking()
        } else showPermDialog()
    }

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        setupUI()
        observeVM()
    }

    private fun setupUI() {
        binding.btnToggle.setOnClickListener {
            if (LocationTrackingService.isRunning) stopTracking() else checkPermAndStart()
        }
        binding.intervalSlider.addOnChangeListener { _: Slider, v: Float, _: Boolean ->
            intervalMs = v.toLong() * 1000L
            binding.tvInterval.text = "Update every ${v.toInt()}s"
        }
        binding.btnMap.setOnClickListener { startActivity(Intent(this, MapActivity::class.java)) }
        binding.btnHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.btnDashboard.setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
        binding.btnGeofence.setOnClickListener { startActivity(Intent(this, GeofenceActivity::class.java)) }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnClear.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Clear all history?")
                .setMessage("This will delete all recorded location points.")
                .setPositiveButton("Delete") { _, _ ->
                    vm.clearHistory()
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }.setNegativeButton("Cancel", null).show()
        }
    }

    private fun observeVM() {
        vm.current.observe(this) { loc ->
            if (loc == null) {
                binding.tvCoords.text = "-- , --"
                binding.tvAddress.text = "No location yet"
                return@observe
            }
            binding.tvCoords.text = loc.formattedCoords()
            binding.tvAddress.text = loc.address.ifEmpty { "Address unavailable" }
            binding.tvSpeed.text = loc.formattedSpeed()
            binding.tvAlt.text = loc.formattedAltitude()
            binding.tvAccuracy.text = loc.formattedAccuracy()
            binding.tvTime.text = loc.formattedTime()
        }
        vm.stats.observe(this) { s ->
            binding.tvPoints.text = "${s.pointCount} pts"
            binding.tvDistance.text = s.formattedDistance()
        }
        vm.isTracking.observe(this) { tracking ->
            binding.btnToggle.text = if (tracking) "⏹  Stop Tracking" else "▶  Start Tracking"
            binding.btnToggle.backgroundTintList = androidx.core.content.res.ResourcesCompat.getColorStateList(
                resources,
                if (tracking) android.R.color.holo_red_dark else R.color.tracking_green,
                theme
            )
            binding.tvStatusBadge.text = if (tracking) "● LIVE" else "○ IDLE"
            binding.tvStatusBadge.setTextColor(
                if (tracking) getColor(R.color.tracking_green) else getColor(android.R.color.darker_gray)
            )
        }
    }

    private fun checkPermAndStart() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) { doStartTracking(); return }
        permLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun doStartTracking() {
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
            putExtra(LocationTrackingService.EXTRA_INTERVAL_MS, intervalMs)
            putExtra(LocationTrackingService.EXTRA_SESSION, vm.repo.newSessionId())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        vm.setTracking(true)
    }

    private fun stopTracking() {
        startService(Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        })
        vm.setTracking(false)
    }

    private fun showPermDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("Please grant location permission to track your position.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)))
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onResume() {
        super.onResume()
        vm.setTracking(LocationTrackingService.isRunning)
    }
}
