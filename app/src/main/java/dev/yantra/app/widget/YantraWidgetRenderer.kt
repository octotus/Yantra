package dev.yantra.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import dev.yantra.app.calendar.CalendarCatalog
import dev.yantra.app.calendar.YantraState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object YantraWidgetRenderer {
    private const val SIZE = 720
    private const val GOLD = 0xFFE6B85C.toInt()
    private const val DIM_GOLD = 0xFF8C6828.toInt()
    private const val BRONZE = 0xFF6F3D1F.toInt()
    private const val IVORY = 0xFFFFE7B0.toInt()
    private const val INK = 0xFF070604.toInt()
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    fun render(state: YantraState, dateTime: ZonedDateTime): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centerX = SIZE / 2f
        val centerY = SIZE / 2f
        val radius = SIZE * 0.43f

        drawTimedBackground(canvas, state)
        drawWatchBody(canvas, centerX, centerY, radius)
        drawYantra(canvas, state, centerX, centerY, radius)
        drawCurvedText(canvas, state.samvatsara.name, centerX, centerY, radius * 1.15f, true, SIZE * 0.04f * 1.5f)
        drawCurvedText(canvas, dateFormatter.format(dateTime), centerX, centerY, radius + 58f, false, SIZE * 0.038f)
        drawMoon(canvas, state, centerX, centerY, radius * 0.19f)
        drawHands(canvas, state, centerX, centerY, radius)
        return bitmap
    }

    private fun drawTimedBackground(canvas: Canvas, state: YantraState) {
        val dayColor = when {
            state.solarAltitude > 35.0 -> 0xFFFAE8A3.toInt()
            state.solarAltitude > 5.0 -> 0xFFB97831.toInt()
            state.solarAltitude > -8.0 -> 0xFF5B3E5F.toInt()
            state.solarAltitude > -18.0 -> 0xFF152D5E.toInt()
            else -> 0xFF06122C.toInt()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            SIZE.toFloat(),
            dayColor,
            INK,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), paint)

        if (state.solarAltitude < -8.0) {
            paint.shader = null
            paint.color = Color.argb(170, 255, 236, 184)
            listOf(
                88f to 112f,
                172f to 268f,
                548f to 146f,
                610f to 324f,
                486f to 514f,
                118f to 486f,
            ).forEach { (x, y) -> canvas.drawCircle(x, y, 2.4f, paint) }
        }
    }

    private fun drawWatchBody(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx - radius * 0.25f,
            cy - radius * 0.35f,
            radius * 1.25f,
            intArrayOf(0xFF2B2114.toInt(), 0xFF0B0906.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius + 54f, paint)

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = GOLD
        paint.strokeWidth = 7f
        canvas.drawCircle(cx, cy, radius + 49f, paint)
        paint.color = BRONZE
        paint.strokeWidth = 18f
        canvas.drawCircle(cx, cy, radius + 35f, paint)
        paint.color = Color.argb(72, 255, 255, 255)
        paint.strokeWidth = 5f
        canvas.drawArc(RectF(cx - radius - 27f, cy - radius - 27f, cx + radius + 27f, cy + radius + 27f), 205f, 115f, false, paint)
    }

    private fun drawYantra(canvas: Canvas, state: YantraState, cx: Float, cy: Float, radius: Float) {
        drawTithiRing(canvas, state, cx, cy, radius * 0.94f)
        drawNakshatraRing(canvas, state, cx, cy, radius * 0.75f)
        drawMasaRing(canvas, state, cx, cy, radius * 0.57f)
        drawRashiRing(canvas, state, cx, cy, radius * 0.39f)
    }

    private fun drawTithiRing(canvas: Canvas, state: YantraState, cx: Float, cy: Float, ringRadius: Float) {
        val paint = ringPaint(2.4f, DIM_GOLD)
        canvas.drawCircle(cx, cy, ringRadius, paint)
        val textPaint = textPaint(ringRadius * 0.105f, GOLD, Paint.Align.CENTER)
        for (index in 0 until 30) {
            val angle = index * 12.0
            val point = polar(cx, cy, ringRadius, angle)
            if (index == 0 || index == 15) {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = if (index == 0) Paint.Style.FILL else Paint.Style.STROKE
                    strokeWidth = 3f
                    color = GOLD
                }
                canvas.drawCircle(point.first, point.second, 8f, dotPaint)
            } else {
                canvas.drawText(((index % 15) + 1).toString(), point.first, point.second + textPaint.textSize * 0.34f, textPaint)
            }
        }
        drawActiveArc(canvas, ringRadius, state.tithi.index * 12f, 12f, cx, cy)
    }

    private fun drawNakshatraRing(canvas: Canvas, state: YantraState, cx: Float, cy: Float, ringRadius: Float) {
        val paint = ringPaint(2.2f, DIM_GOLD)
        canvas.drawCircle(cx, cy, ringRadius, paint)
        val active = state.nakshatra.index
        val textPaint = textPaint(ringRadius * 0.095f, GOLD, Paint.Align.CENTER)
        for (index in CalendarCatalog.nakshatras.indices) {
            val point = polar(cx, cy, ringRadius, index * 360.0 / 27.0)
            val label = CalendarCatalog.nakshatras[index].name.take(2).uppercase()
            textPaint.color = if (index == active) IVORY else GOLD
            textPaint.textSize = if (index == active) ringRadius * 0.125f else ringRadius * 0.092f
            canvas.drawText(label, point.first, point.second + textPaint.textSize * 0.33f, textPaint)
        }
        drawActiveArc(canvas, ringRadius, active * 360f / 27f, 360f / 27f, cx, cy)
    }

    private fun drawMasaRing(canvas: Canvas, state: YantraState, cx: Float, cy: Float, ringRadius: Float) {
        val paint = ringPaint(2.2f, DIM_GOLD)
        canvas.drawCircle(cx, cy, ringRadius, paint)
        val textPaint = textPaint(ringRadius * 0.13f, GOLD, Paint.Align.CENTER)
        var start = 0f
        state.monthSectors.forEachIndexed { index, month ->
            val sweep = month.arcDegrees.toFloat()
            if (index == state.month.index) drawActiveArc(canvas, ringRadius, start, sweep, cx, cy)
            val point = polar(cx, cy, ringRadius, start + sweep / 2.0)
            canvas.drawText(month.abbreviation, point.first, point.second + textPaint.textSize * 0.33f, textPaint)
            start += sweep
        }
    }

    private fun drawRashiRing(canvas: Canvas, state: YantraState, cx: Float, cy: Float, ringRadius: Float) {
        val paint = ringPaint(2.3f, DIM_GOLD)
        canvas.drawCircle(cx, cy, ringRadius, paint)
        val textPaint = textPaint(ringRadius * 0.19f, GOLD, Paint.Align.CENTER)
        val sectors = state.lagnaSectors.ifEmpty {
            CalendarCatalog.rashis.mapIndexed { index, rashi ->
                dev.yantra.app.calendar.LagnaSector(index, rashi.name, index / 12.0, 1.0 / 12.0)
            }
        }
        sectors.forEach { sector ->
            val start = (sector.startFraction * 360.0).toFloat()
            val sweep = (sector.durationFraction * 360.0).toFloat()
            if (sector.index == state.rashi.index || sector.index == state.lagnaRashi?.index) {
                drawActiveArc(canvas, ringRadius, start, sweep, cx, cy)
            }
            val point = polar(cx, cy, ringRadius, start + sweep / 2.0)
            val label = CalendarCatalog.rashis[sector.index].name.take(2).uppercase()
            textPaint.color = if (sector.index == state.rashi.index) IVORY else GOLD
            canvas.drawText(label, point.first, point.second + textPaint.textSize * 0.34f, textPaint)
        }
    }

    private fun drawMoon(canvas: Canvas, state: YantraState, cx: Float, cy: Float, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        paint.color = 0xFF11141D.toInt()
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = 0xFFEDE7D3.toInt()
        val illumination = state.moonIllumination.toFloat().coerceIn(0.04f, 0.96f)
        val waxing = state.tithi.index < 15
        val litWidth = radius * 2f * illumination
        val left = if (waxing) cx + radius - litWidth else cx - radius
        canvas.save()
        canvas.clipPath(Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) })
        canvas.drawOval(RectF(left, cy - radius, left + litWidth, cy + radius), paint)
        canvas.restore()

        val stroke = ringPaint(2f, GOLD)
        canvas.drawCircle(cx, cy, radius, stroke)
        val label = "${state.paksha} ${state.tithi.index % 15 + 1}"
        val textPaint = textPaint(radius * 0.28f, IVORY, Paint.Align.CENTER)
        canvas.drawText(label, cx, cy + radius * 1.55f, textPaint)
    }

    private fun drawHands(canvas: Canvas, state: YantraState, cx: Float, cy: Float, radius: Float) {
        val tithiAngle = state.tithi.index * 12.0
        drawHand(canvas, cx, cy, radius * 0.93f, tithiAngle, 6f, GOLD, 18f)

        val lagnaAngle = state.lagnaDayFraction * 360.0
        drawHand(canvas, cx, cy, radius * 0.39f, lagnaAngle, 5f, 0xFFFFD783.toInt(), 18f)
    }

    private fun drawHand(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        length: Float,
        angleDegrees: Double,
        strokeWidth: Float,
        color: Int,
        arrowSize: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            this.strokeWidth = strokeWidth
            this.color = color
        }
        val tip = polar(cx, cy, length, angleDegrees)
        canvas.drawLine(cx, cy, tip.first, tip.second, paint)
        paint.style = Paint.Style.FILL
        val angle = Math.toRadians(angleDegrees - 90.0)
        val left = angle + PI * 0.82
        val right = angle - PI * 0.82
        val path = Path().apply {
            moveTo(tip.first, tip.second)
            lineTo((tip.first + cos(left) * arrowSize).toFloat(), (tip.second + sin(left) * arrowSize).toFloat())
            lineTo((tip.first + cos(right) * arrowSize).toFloat(), (tip.second + sin(right) * arrowSize).toFloat())
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCurvedText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        radius: Float,
        top: Boolean,
        textSize: Float,
    ) {
        val paint = textPaint(textSize, IVORY, Paint.Align.CENTER)
        val path = Path()
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        if (top) {
            path.addArc(rect, 202f, 136f)
        } else {
            path.addArc(rect, 22f, 136f)
        }
        canvas.drawTextOnPath(text, path, 0f, if (top) -4f else 18f, paint)
    }

    private fun drawActiveArc(canvas: Canvas, radius: Float, startAngle: Float, sweepAngle: Float, cx: Float, cy: Float) {
        val paint = ringPaint(7f, GOLD)
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, startAngle - 90f, sweepAngle, false, paint)
    }

    private fun ringPaint(width: Float, color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = width
            this.color = color
        }

    private fun textPaint(size: Float, color: Int, align: Paint.Align): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
            textAlign = align
            textSize = size
            this.color = color
        }

    private fun polar(cx: Float, cy: Float, radius: Float, degreesFromTop: Double): Pair<Float, Float> {
        val radians = Math.toRadians(degreesFromTop - 90.0)
        return Pair(
            (cx + cos(radians) * radius).toFloat(),
            (cy + sin(radians) * radius).toFloat(),
        )
    }
}
