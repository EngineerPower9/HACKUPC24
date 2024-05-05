package com.example.prova4

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SkyPlotView : View {
    private val satellitePositions = mutableMapOf<Int, Pair<Float, Float>>() // <SvId, <azimuth, elevation>>

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setSatellitePositions(positions: Map<Int, Pair<Float, Float>>) {
        satellitePositions.clear()
        satellitePositions.putAll(positions)
        invalidate() // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas?.let {
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (Math.min(width, height) / 2f) * 0.9f // 90% of the min dimension
            val horizonRadius = (Math.min(width, height) / 2f) * 0.91f // 80% of the min dimension

            val paint = Paint()
            paint.color = Color.WHITE
            paint.strokeWidth = 2f


            // Draw the white circle representing the horizon
            it.drawCircle(centerX, centerY, horizonRadius, paint)

            // Draw the skyplot circle
            paint.color = Color.BLACK
            it.drawCircle(centerX, centerY, radius, paint)



            // Draw lines for each 30-degree azimuth angle
            paint.color = Color.WHITE
            for (angle in 0 until 360 step 30) {
                val angleRad = Math.toRadians(angle.toDouble())
                val startX = centerX + radius * Math.cos(angleRad).toFloat()
                val startY = centerY - radius * Math.sin(angleRad).toFloat()
                val endX = centerX + radius * Math.cos(angleRad + Math.PI).toFloat()
                val endY = centerY - radius * Math.sin(angleRad + Math.PI).toFloat()
                it.drawLine(startX, startY, endX, endY, paint)
            }

            // Draw satellites
            paint.color = Color.RED
            paint.strokeWidth = 15f
            satellitePositions.forEach { (_, position) ->
                val (azimuth, elevation) = position
                val azimuthRad = Math.toRadians(azimuth.toDouble())
                val elevationRad = Math.toRadians(elevation.toDouble())
                val satelliteX = centerX + radius * Math.cos(elevationRad) * Math.sin(azimuthRad)
                val satelliteY = centerY - radius * Math.cos(elevationRad) * Math.cos(azimuthRad)
                it.drawPoint(satelliteX.toFloat(), satelliteY.toFloat(), paint)
            }
        }
    }
}
