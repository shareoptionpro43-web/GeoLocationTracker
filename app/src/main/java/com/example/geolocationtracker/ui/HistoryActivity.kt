package com.example.geolocationtracker.ui

import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geolocationtracker.data.LocationEntity
import com.example.geolocationtracker.databinding.ActivityHistoryBinding
import com.example.geolocationtracker.databinding.ItemLocationBinding
import com.example.geolocationtracker.utils.CsvExporter
import android.widget.Toast

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private val vm: LocationViewModel by viewModels()
    private val adapter = HistoryAdapter { vm.delete(it) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "History"
        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = adapter
        vm.recentLocations.observe(this) {
            adapter.submit(it)
            binding.tvCount.text = "${it.size} records"
        }
        binding.btnExport.setOnClickListener {
            val locs = vm.recentLocations.value ?: emptyList()
            if (locs.isEmpty()) { Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val uri = CsvExporter.export(this, locs)
            if (uri != null) CsvExporter.share(this, uri)
            else Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}

class HistoryAdapter(private val onDelete: (LocationEntity) -> Unit) :
    RecyclerView.Adapter<HistoryAdapter.VH>() {
    private var items = listOf<LocationEntity>()
    fun submit(list: List<LocationEntity>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemLocationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemLocationBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val loc = items[i]
        h.b.tvCoords.text = loc.formattedCoords()
        h.b.tvTime.text = loc.formattedTime()
        h.b.tvSpeed.text = loc.formattedSpeed()
        h.b.tvAccuracy.text = loc.formattedAccuracy()
        h.b.tvAddress.text = loc.address.ifEmpty { "No address" }
        h.b.btnDelete.setOnClickListener { onDelete(loc) }
    }
}
