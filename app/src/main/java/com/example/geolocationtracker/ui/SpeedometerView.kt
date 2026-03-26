package com.example.geolocationtracker.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class SpeedometerView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null, def: Int = 0
) : View(ctx, attrs, def) {

    private var speed = 0f
    private val maxSpeed = 140f
    private val startAngle = 135f
    private val sweep = 270f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A2E40") }
    private val arcBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D3347"); style = Paint.Style.STROKE; strokeWidth = 22f; strokeCap = Paint.Cap.ROUND
    }
    private val arcFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 22f; strokeCap = Paint.Cap.ROUND
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF5350"); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF5350") }
    private val dotCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A2E40") }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2D4A5E"); strokeWidth = 2.5f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#546E7A"); textAlign = Paint.Align.CENTER; textSize = 18f
    }
    private val speedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 56f; typeface = Typeface.DEFAULT_BOLD
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90A4AE"); textAlign = Paint.Align.CENTER; textSize = 20f
    }

    fun setSpeed(kmh: Float) {
        speed = kmh.coerceIn(0f, maxSpeed)
        val r = speed / maxSpeed
        arcFgPaint.color = when {
            r < 0.45f -> Color.parseColor("#4CAF50")
            r < 0.75f -> Color.parseColor("#FFC107")
            else      -> Color.parseColor("#F44336")
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(cx, cy) * 0.88f

        canvas.drawCircle(cx, cy, r, bgPaint)

        val pad = 28f
        val oval = RectF(cx - r + pad, cy - r + pad, cx + r - pad, cy + r - pad)
        canvas.drawArc(oval, startAngle, sweep, false, arcBgPaint)
        val fgSweep = sweep * (speed / maxSpeed)
        if (fgSweep > 0f) canvas.drawArc(oval, startAngle, fgSweep, false, arcFgPaint)

        // Ticks
        val tickCount = 14
        for (i in 0..tickCount) {
            val angle = Math.toRadians((startAngle + sweep / tickCount * i).toDouble())
            val inner = r - 52f; val outer = r - 30f
            canvas.drawLine(
                (cx + inner * cos(angle)).toFloat(), (cy + inner * sin(angle)).toFloat(),
                (cx + outer * cos(angle)).toFloat(), (cy + outer * sin(angle)).toFloat(), tickPaint
            )
            if (i % 2 == 0) {
                val lr = r - 72f
                val spd = (maxSpeed / tickCount * i).toInt()
                canvas.drawText("$spd", (cx + lr * cos(angle)).toFloat(),
                    (cy + lr * sin(angle)).toFloat() + 7f, labelPaint)
            }
        }

        // Needle
        val needleAngle = Math.toRadians((startAngle + sweep * (speed / maxSpeed)).toDouble())
        val nl = r - 56f
        canvas.drawLine(cx, cy,
            (cx + nl * cos(needleAngle)).toFloat(), (cy + nl * sin(needleAngle)).toFloat(), needlePaint)

        // Center dot
        canvas.drawCircle(cx, cy, 14f, dotPaint)
        canvas.drawCircle(cx, cy, 8f, dotCenterPaint)

        // Speed text
        canvas.drawText("${speed.toInt()}", cx, cy + r * 0.38f, speedPaint)
        canvas.drawText("km/h", cx, cy + r * 0.38f + 30f, unitPaint)
    }
}
