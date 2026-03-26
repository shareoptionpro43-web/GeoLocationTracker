package com.example.geolocationtracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.geolocationtracker.data.LocationEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun export(context: Context, locations: List<LocationEntity>): Uri? {
        return try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
            dir.mkdirs()
            val file = File(dir, "geo_track_$stamp.csv")
            FileWriter(file).use { w ->
                w.appendLine("ID,Timestamp,Latitude,Longitude,Altitude(m),Accuracy(m),Speed(km/h),Address,Session")
                locations.forEach { loc ->
                    w.appendLine("${loc.id},\"${loc.formattedTime()}\",${loc.latitude},${loc.longitude}," +
                        "${"%.2f".format(loc.altitude)},${"%.2f".format(loc.accuracy)}," +
                        "${"%.2f".format(loc.speedKmh())},\"${loc.address.replace("\"", "'")}\",${loc.sessionId}")
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    fun share(context: Context, uri: Uri) {
        context.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Geo Location History Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share CSV"))
    }
}
