package dev.yantra.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.yantra.app.calendar.CalendarCatalog
import dev.yantra.app.calendar.MonthNameSet
import dev.yantra.app.calendar.YantraState
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min

internal data class YantraLayout(
    val center: Offset,
    val radius: Float,
    val dateHitRect: Rect,
    val settingsHitRect: Rect,
    val lotusCenter: Offset,
    val lotusRadius: Float,
)

internal enum class AnnotationKind {
    Rashi,
    Masa,
    Nakshatra,
}

internal data class YantraAnnotation(
    val kind: AnnotationKind,
    val index: Int,
    val name: String,
)

internal fun yantraLayout(width: Float, height: Float): YantraLayout {
    val radius = min(width * 0.462f, height * 0.34f)
    val ringWidth = radius * 0.135f
    val tithiRingWidth = ringWidth * 1.15f
    val tithiRingRadius = radius - ringWidth * 0.35f
    val faceRadius = tithiRingRadius + tithiRingWidth * 0.42f
    val bodyRadius = faceRadius + ringWidth * 0.38f
    val topTextOffset = ringWidth * 0.48f
    val bottomTextOffset = ringWidth * 1.72f + lotusRadiusForLayout(ringWidth) + radius * 0.2592f
    val centerY = min(height * 0.47f, height - bodyRadius - bottomTextOffset - ringWidth * 1.1f)
        .coerceAtLeast(bodyRadius + topTextOffset + ringWidth * 0.65f)
    val center = Offset(width / 2f, centerY)
    val lotusCenter = Offset(center.x, center.y + bodyRadius + ringWidth * 1.72f)
    val lotusRadius = lotusRadiusForLayout(ringWidth)
    val dateCenter = Offset(center.x, center.y + bodyRadius + bottomTextOffset)
    val dateHitRect = Rect(
        dateCenter.x - min(width * 0.44f, radius * 0.95f),
        dateCenter.y - ringWidth * 0.62f,
        dateCenter.x + min(width * 0.44f, radius * 0.95f),
        dateCenter.y + ringWidth * 0.62f,
    )
    val gearSize = min(width, height) * 0.0665f
    val gearCenter = Offset(width * 0.09f, height - gearSize * 2.35f)
    val settingsHitRect = Rect(
        gearCenter.x - gearSize * 0.72f,
        gearCenter.y - gearSize * 0.72f,
        gearCenter.x + gearSize * 0.72f,
        gearCenter.y + gearSize * 0.72f,
    )
    return YantraLayout(center, radius, dateHitRect, settingsHitRect, lotusCenter, lotusRadius)
}

internal fun lotusRadiusForLayout(ringWidth: Float): Float = ringWidth * 0.76f

internal fun hitAnnotation(
    tap: Offset,
    center: Offset,
    ringWidth: Float,
    nakshatraRingRadius: Float,
    monthRingRadius: Float,
    rashiRingRadius: Float,
    state: YantraState,
    monthNameSet: MonthNameSet,
): YantraAnnotation? {
    val distance = hypot(tap.x - center.x, tap.y - center.y)
    return when {
        distance.isInRing(rashiRingRadius, ringWidth) -> {
            val fraction = angleToFraction(tap, center)
            val index = state.lagnaSectors.firstOrNull { sector ->
                fraction >= sector.startFraction && fraction < sector.startFraction + sector.durationFraction
            }?.index ?: angleToIndex(tap, center, 12)
            val rashi = CalendarCatalog.rashis[index]
            YantraAnnotation(AnnotationKind.Rashi, index, rashi.name)
        }
        distance.isInRing(monthRingRadius, ringWidth) -> {
            val fraction = angleToFraction(tap, center)
            val sectors = localizedMonthSectors(state.monthSectors, monthNameSet)
            val sector = sectors.firstOrNull { month ->
                val start = sectors.takeWhile { it.index != month.index }.sumOf { it.arcDegrees } / 360.0
                fraction >= start && fraction < start + month.arcDegrees / 360.0
            } ?: sectors.lastOrNull()
            sector?.let { YantraAnnotation(AnnotationKind.Masa, it.index, it.name) }
        }
        distance.isInRing(nakshatraRingRadius, ringWidth) -> {
            val index = angleToIndex(tap, center, 27)
            val nakshatra = CalendarCatalog.nakshatras[index]
            YantraAnnotation(AnnotationKind.Nakshatra, index, nakshatra.name)
        }
        else -> null
    }
}

private fun Float.isInRing(radius: Float, width: Float): Boolean =
    this >= radius - width * 0.56f && this <= radius + width * 0.56f

private fun angleToFraction(tap: Offset, center: Offset): Double {
    val angle = Math.toDegrees(kotlin.math.atan2((tap.y - center.y).toDouble(), (tap.x - center.x).toDouble()))
    return ((angle + 90.0 + 360.0) % 360.0) / 360.0
}

private fun angleToIndex(tap: Offset, center: Offset, count: Int): Int =
    floor(angleToFraction(tap, center) * count).toInt().coerceIn(0, count - 1)
