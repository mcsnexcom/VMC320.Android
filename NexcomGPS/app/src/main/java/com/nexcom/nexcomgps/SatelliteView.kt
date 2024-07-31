package com.nexcom.nexcomgps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.GnssStatus
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class SatelliteView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var satellites: MutableMap<Int, SatelliteInfo> = mutableMapOf()
    private var paint: Paint = Paint()
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var gpsTime: String = ""

    data class SatelliteInfo(
        val azimuth: Float,
        val elevation: Float,
        val usedInFix: Boolean
    )

    init {
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width
        val height = height
        val centerX = width / 2
        val centerY = height / 2
        val radius = Math.min(centerX, centerY) - 20

        // Draw the radar background
        paint.color = Color.WHITE
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), paint)

        // Draw the radar lines and circles
        paint.color = Color.BLACK
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(centerX.toFloat(), (centerY - radius).toFloat(), centerX.toFloat(), (centerY + radius).toFloat(), paint)
        canvas.drawLine((centerX - radius).toFloat(), centerY.toFloat(), (centerX + radius).toFloat(), centerY.toFloat(), paint)

        // Draw 5 concentric circles
        for (i in 1..5) {
            canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), (radius * i / 5).toFloat(), paint)
        }

        // Reset paint style for text and satellites
        paint.style = Paint.Style.FILL

        // Draw the satellites on the radar
        satellites.forEach { (id, satellite) ->
            val azimuth = Math.toRadians(satellite.azimuth.toDouble())
            val elevation = Math.toRadians(satellite.elevation.toDouble())

            val x = (centerX + radius * Math.cos(azimuth) * Math.cos(elevation)).toFloat()
            val y = (centerY + radius * Math.sin(azimuth) * Math.cos(elevation)).toFloat()

            paint.color = if (satellite.usedInFix) Color.GREEN else Color.RED
            canvas.drawCircle(x, y, 10f, paint)
        }

        // Draw the text information
        paint.color = Color.BLACK
        paint.textSize = 30f
        canvas.drawText("GPS Information", 20f, height - 20f, paint)

        paint.textSize = 20f
        canvas.drawText("Latitude: $latitude", 20f, height - 60f, paint)
        canvas.drawText("Longitude: $longitude", 20f, height - 40f, paint)
        canvas.drawText("GPS Time: $gpsTime", 20f, height - 80f, paint)
    }

    fun updateSatellites(status: GnssStatus) {
        if (status.satelliteCount > 0) {
            satellites.clear()
            for (i in 0 until status.satelliteCount) {
                satellites[i] = SatelliteInfo(
                    status.getAzimuthDegrees(i),
                    status.getElevationDegrees(i),
                    status.usedInFix(i)
                )
            }
            invalidate()
        }
    }

    fun updateCoordinates(lat: Double, lon: Double, time: Long) {
        latitude = lat
        longitude = lon

        // Update GPS time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        gpsTime = dateFormat.format(Date(time))

        invalidate()
    }
}
