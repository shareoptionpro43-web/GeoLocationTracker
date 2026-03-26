package com.example.geolocationtracker.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.geolocationtracker.databinding.ActivityDashboardBinding
import com.example.geolocationtracker.utils.CsvExporter

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val vm: LocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Dashboard"

        observeData()
        setupButtons()
    }

    private fun observeData() {
        vm.current.observe(this) { loc ->
            loc ?: return@observe
            binding.speedometer.setSpeed(loc.speedKmh())
            binding.tvCurrentSpeed.text = loc.formattedSpeed()
            binding.tvAltD.text = loc.formattedAltitude()
            binding.tvAccD.text = loc.formattedAccuracy()
            binding.tvAddressD.text = loc.address.ifEmpty { "Calculating address..." }
        }

        vm.stats.observe(this) { s ->
            binding.tvMaxSpeed.text = "${"%.1f".format(s.maxSpeedKmh)} km/h"
            binding.tvAvgSpeed.text = "${"%.1f".format(s.avgSpeedKmh)} km/h"
            binding.tvTotalDist.text = s.formattedDistance()
            binding.tvDuration.text = s.formattedDuration()
            binding.tvPointsD.text = "${s.pointCount}"
        }
    }

    private fun setupButtons() {
        binding.btnExportCsv.setOnClickListener {
            val locs = vm.recentLocations.value ?: emptyList()
            if (locs.isEmpty()) { toast("No data to export"); return@setOnClickListener }
            val uri = CsvExporter.export(this, locs)
            if (uri != null) CsvExporter.share(this, uri) else toast("Export failed")
        }

        binding.btnShareLoc.setOnClickListener {
            val loc = vm.current.value ?: run { toast("No location yet"); return@setOnClickListener }
            val text = "📍 My Location\n${loc.formattedCoords()}\n${loc.address}\nhttps://maps.google.com/?q=${loc.latitude},${loc.longitude}"
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
            }, "Share location"))
        }

        binding.btnOpenMaps.setOnClickListener {
            val loc = vm.current.value ?: run { toast("No location yet"); return@setOnClickListener }
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}")))
        }

        binding.btnNavigate.setOnClickListener {
            val loc = vm.current.value ?: run { toast("No location yet"); return@setOnClickListener }
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=${loc.latitude},${loc.longitude}")))
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
