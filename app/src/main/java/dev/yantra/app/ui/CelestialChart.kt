package dev.yantra.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import dev.yantra.app.calendar.CalendarCatalog
import dev.yantra.app.engine.*
import kotlin.math.*

private val ChartGold = Color(0xFFE8C477)
private val ChartGrey = Color(0xFF859095)

/** All detail views use this same fixed, north-up celestial projection. */
@Composable
internal fun CelestialChart(
    catalog: StarCatalog,
    snapshot: CelestialSnapshot,
    frame: ChartFrame,
    kind: AnnotationKind,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val patternKind = if (kind == AnnotationKind.Rashi) "rashi" else "nakshatra"
    val count = if (patternKind == "rashi") 12 else 27
    val index = if (kind == AnnotationKind.Masa) floor(snapshot.lunarLongitude / (360.0 / 27.0)).toInt() else selectedIndex
    val neighbors = listOf(Math.floorMod(index - 1, count), index, (index + 1) % count)
    val patterns = neighbors.map { catalog.pattern(patternKind, it) }
    val selectedIds = patterns[1].stars.toSet()
    val patternIds = patterns.flatMap { it.stars }.toSet()
    val projection = remember(frame) { StereographicProjection(frame.center) }
    val projected = remember(catalog, frame) { catalog.stars.mapNotNull { star -> projection.project(star.coordinate)?.let { star to it } } }
    var tappedStar by remember(frame, kind, index) { mutableStateOf<Int?>(null) }

    Canvas(modifier.pointerInput(frame, catalog) {
        detectTapGestures { tap ->
            val scale = min(size.width, size.height) / (2.0 * frame.extent)
            tappedStar = projected.map { (star, p) ->
                star.id to hypot(tap.x - (size.width / 2.0 + p.x * scale), tap.y - (size.height / 2.0 + p.y * scale))
            }.filter { it.second < 22 * density }.minByOrNull { it.second }?.first
        }
    }) {
        val scale = min(size.width, size.height) / (2.0 * frame.extent)
        fun pixel(p: ChartPoint) = Offset((center.x + p.x * scale).toFloat(), (center.y + p.y * scale).toFloat())
        fun position(c: EquatorialCoordinate) = projection.project(c)?.let(::pixel)
        fun visible(p: Offset, margin: Float = 0f) = p.x >= margin && p.x <= size.width - margin && p.y >= margin && p.y <= size.height - margin
        val positions = projected.associate { (s, p) -> s.id to pixel(p) }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("serif", Typeface.NORMAL)
            textSize = 12 * density
        }
        val occupied = mutableListOf<android.graphics.RectF>()
        fun label(text: String, point: Offset, color: Color, centered: Boolean = true) {
            val width = labelPaint.measureText(text)
            val x = (if (centered) point.x - width / 2 else point.x).coerceIn(8 * density, max(8 * density, size.width - width - 8 * density))
            val y = point.y.coerceIn(18 * density, size.height - 10 * density)
            val box = android.graphics.RectF(x - 4, y - labelPaint.textSize - 3, x + width + 4, y + 5)
            if (occupied.any { android.graphics.RectF.intersects(it, box) }) return
            occupied += box
            labelPaint.color = color.toArgb()
            drawContext.canvas.nativeCanvas.drawText(text, x, y, labelPaint)
        }

        drawRect(Color.Black)
        clipRect {
            projected.forEach { (star, p) ->
                val at = pixel(p)
                if (visible(at) && star.id !in patternIds) {
                    val alpha = ((6.5 - star.magnitude) / 8.0).coerceIn(0.12, 0.65).toFloat()
                    drawCircle(Color(0xFFD1D8DE).copy(alpha = alpha), ((6.5 - star.magnitude) * 0.27).coerceIn(0.5, 1.5).toFloat() * density, at)
                }
            }
            patterns.forEach { pattern ->
                val selected = pattern.index == index
                pattern.edges.forEach { (a, b) ->
                    val from = positions[a]; val to = positions[b]
                    if (from != null && to != null) drawLine(if (selected) ChartGold.copy(alpha = 0.82f) else ChartGrey.copy(alpha = 0.38f), from, to, (if (selected) 1.1f else 0.7f) * density)
                }
            }
            fun curve(start: Double, end: Double, color: Color, width: Float) {
                var previous: Offset? = null
                val steps = ceil((end - start) * 2).toInt()
                for (i in 0..steps) {
                    val p = position(snapshot.eclipticAt(start + (end - start) * i / steps.coerceAtLeast(1)))
                    if (p != null && previous != null && (p - previous!!).getDistance() < size.width / 3) drawLine(color, previous!!, p, width)
                    previous = p
                }
            }
            curve(0.0, 360.0, ChartGrey.copy(alpha = 0.55f), 0.7f * density)
            if (kind != AnnotationKind.Masa) {
                val width = 360.0 / count
                val start = index * width
                curve(start, start + width, ChartGold, 2.3f * density)
                for (longitude in listOf(start, start + width)) position(snapshot.eclipticAt(longitude))?.let { p ->
                    drawCircle(ChartGold, 2.5f * density, p)
                    drawLine(ChartGold.copy(alpha = 0.6f), p - Offset(0f, 7 * density), p + Offset(0f, 7 * density), density)
                }
                position(snapshot.eclipticAt(start + width / 2))?.takeIf { visible(it) }?.let { p ->
                    label(if (kind == AnnotationKind.Rashi) "30°" else "13°20′", p + Offset(0f, 25 * density), ChartGold)
                }
            }
            patternIds.forEach { id ->
                val star = catalog.byId.getValue(id)
                positions[id]?.takeIf { visible(it) }?.let { at ->
                    val primary = id in selectedIds
                    val radius = ((6.5 - star.magnitude) * 0.48).coerceIn(1.3, 3.2).toFloat() * density
                    val color = if (primary) ChartGold else ChartGrey
                    if (primary) drawCircle(color.copy(alpha = 0.12f), radius * 2.7f, at)
                    drawCircle(color, radius, at)
                }
            }
            val moon = position(snapshot.moon)
            if (moon != null && visible(moon, 16 * density)) {
                val raDifference = toRadians(snapshot.sun.ra - snapshot.moon.ra)
                val sunDec = toRadians(snapshot.sun.dec)
                val moonDec = toRadians(snapshot.moon.dec)
                val east = cos(sunDec) * sin(raDifference)
                val north = sin(sunDec) * cos(moonDec) - cos(sunDec) * sin(moonDec) * cos(raDifference)
                val angle = Math.toDegrees(atan2(-north, -east)).toFloat()
                val radius = (if (kind == AnnotationKind.Masa) 23 else 11) * density
                drawPhaseMoon(moon, radius, snapshot.illumination, angle)
                label("Chandra", moon + Offset(0f, radius + 19 * density), Color(0xFFDCE3EA))
            }
            position(snapshot.sun)?.takeIf { visible(it, 10 * density) }?.let { p ->
                drawCircle(ChartGold.copy(alpha = 0.15f), 9 * density, p)
                drawCircle(ChartGold, 4 * density, p)
                label("Surya", p + Offset(0f, -14 * density), ChartGold)
            }
            // Label the selected pattern before its neighbours to give it priority.
            listOf(patterns[1], patterns[0], patterns[2]).forEach { pattern ->
                val coordinates = pattern.stars.map { catalog.byId.getValue(it).coordinate }
                position(coordinateCenter(coordinates))?.takeIf { visible(it, 10 * density) }?.let { p ->
                    val name = if (patternKind == "rashi") CalendarCatalog.rashis[pattern.index].name else CalendarCatalog.nakshatras[pattern.index].name
                    label(name, p + Offset(0f, -23 * density), if (pattern.index == index) ChartGold else ChartGrey)
                }
            }
            tappedStar?.let { id -> positions[id]?.takeIf { visible(it) }?.let { p ->
                drawCircle(Color.White, 6 * density, p, style = Stroke(density))
                label("HIP $id", p + Offset(0f, 22 * density), Color.White)
            } }
        }
        label("N ↑", Offset(22 * density, 22 * density), ChartGrey, false)
        label("E ←", Offset(22 * density, size.height - 16 * density), ChartGrey, false)
    }
}

/** Illuminated fraction determines the terminator; the bright limb points toward the Sun. */
private fun DrawScope.drawPhaseMoon(at: Offset, radius: Float, fraction: Double, angle: Float) {
    drawCircle(Color(0xFF151B20), radius, at)
    rotate(angle, at) {
        val path = Path()
        val k = (1.0 - 2.0 * fraction).toFloat()
        for (i in 0..80) {
            val y = -radius + 2 * radius * i / 80f
            val x = sqrt(max(0f, radius * radius - y * y))
            if (i == 0) path.moveTo(at.x + x, at.y + y) else path.lineTo(at.x + x, at.y + y)
        }
        for (i in 80 downTo 0) {
            val y = -radius + 2 * radius * i / 80f
            val x = sqrt(max(0f, radius * radius - y * y)) * k
            path.lineTo(at.x + x, at.y + y)
        }
        path.close()
        drawPath(path, Color(0xFFE6E1CD))
    }
    drawCircle(Color(0xFF85898A), radius, at, style = Stroke(0.7f * density))
}
