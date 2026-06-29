package com.project.phonemic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Real-time line graph cho RTT, hiển thị 60 điểm gần nhất.
 * Vẽ bằng Canvas — không cần thư viện ngoài.
 */
class BandwidthGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val maxPoints = 60
    private val rttPoints = ArrayDeque<Double>(maxPoints)

    private val bgPaint = Paint().apply { color = Color.parseColor("#1A1A2E") }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#333355"); strokeWidth = 1f; isAntiAlias = true
    }
    private val linePaint = Paint().apply {
        color = Color.parseColor("#4FC3F7"); strokeWidth = 3f
        isAntiAlias = true; style = Paint.Style.STROKE
    }
    private val fillPaint = Paint().apply {
        color = Color.parseColor("#224FC3F7"); isAntiAlias = true; style = Paint.Style.FILL
    }
    private val labelPaint = Paint().apply {
        color = Color.WHITE; textSize = 28f; isAntiAlias = true
    }
    private val axisPaint = Paint().apply {
        color = Color.parseColor("#888888"); textSize = 24f; isAntiAlias = true
    }

    private var maxRtt = 100.0  // dynamic ceiling

    fun addPoint(rttMs: Double) {
        rttPoints.addLast(rttMs)
        if (rttPoints.size > maxPoints) rttPoints.removeFirst()
        maxRtt = (rttPoints.maxOrNull() ?: 100.0).coerceAtLeast(50.0) * 1.2
        postInvalidate()
    }

    fun clear() {
        rttPoints.clear()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val padL = 80f; val padB = 40f; val padT = 20f; val padR = 20f
        val graphW = w - padL - padR
        val graphH = h - padT - padB

        // background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // grid lines (5 horizontal)
        val gridCount = 5
        for (i in 0..gridCount) {
            val y = padT + graphH * i / gridCount
            canvas.drawLine(padL, y, padL + graphW, y, gridPaint)
            val label = "%.0f".format(maxRtt * (gridCount - i) / gridCount)
            canvas.drawText("${label}ms", 4f, y + 8f, axisPaint)
        }

        if (rttPoints.size < 2) return

        val stepX = graphW / (maxPoints - 1)

        fun xOf(i: Int) = padL + (maxPoints - rttPoints.size + i) * stepX
        fun yOf(v: Double) = padT + graphH * (1.0 - v / maxRtt).coerceIn(0.0, 1.0).toFloat()

        // fill area
        val fillPath = Path()
        fillPath.moveTo(xOf(0), h - padB)
        rttPoints.forEachIndexed { i, v -> fillPath.lineTo(xOf(i), yOf(v)) }
        fillPath.lineTo(xOf(rttPoints.size - 1), h - padB)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        // line
        val linePath = Path()
        rttPoints.forEachIndexed { i, v ->
            if (i == 0) linePath.moveTo(xOf(i), yOf(v))
            else linePath.lineTo(xOf(i), yOf(v))
        }
        canvas.drawPath(linePath, linePaint)

        // latest value label
        val latest = rttPoints.last()
        canvas.drawText("RTT: ${"%.1f".format(latest)}ms", padL + 8f, padT + 36f, labelPaint)
    }
}
