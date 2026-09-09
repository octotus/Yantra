package dev.yantra.app.engine

import kotlin.math.*

/** Geocentric equatorial coordinates in the fixed J2000 frame, in degrees. */
data class EquatorialCoordinate(val ra: Double, val dec: Double)

data class ChartPoint(val x: Double, val y: Double)

/** North up, celestial east left. Points beyond the visible hemisphere are excluded. */
class StereographicProjection(val center: EquatorialCoordinate) {
    fun project(point: EquatorialCoordinate): ChartPoint? {
        val d = toRadians(point.dec)
        val d0 = toRadians(center.dec)
        val a = toRadians(point.ra - center.ra)
        val cosine = sin(d0) * sin(d) + cos(d0) * cos(d) * cos(a)
        if (cosine <= 0.0) return null
        val k = 2.0 / (1.0 + cosine)
        return ChartPoint(-k * cos(d) * sin(a), -k * (cos(d0) * sin(d) - sin(d0) * cos(d) * cos(a)))
    }
}

fun coordinateCenter(points: List<EquatorialCoordinate>): EquatorialCoordinate {
    require(points.isNotEmpty())
    val x = points.sumOf { cosDeg(it.dec) * cosDeg(it.ra) }
    val y = points.sumOf { cosDeg(it.dec) * sinDeg(it.ra) }
    val z = points.sumOf { sinDeg(it.dec) }
    return EquatorialCoordinate(normalizeDegrees(Math.toDegrees(atan2(y, x))), Math.toDegrees(atan2(z, hypot(x, y))))
}

data class CelestialSnapshot(
    val sun: EquatorialCoordinate,
    val moon: EquatorialCoordinate,
    val illumination: Double,
    val lunarLongitude: Double,
    val solarLongitude: Double,
    /** J2000 coordinates of the ecliptic of date, sampled at integer sidereal degrees. */
    val ecliptic: List<EquatorialCoordinate>,
) {
    fun eclipticAt(longitude: Double): EquatorialCoordinate {
        val value = normalizeDegrees(longitude)
        val index = floor(value).toInt()
        val fraction = value - index
        val a = ecliptic[index]
        val b = ecliptic[index + 1]
        val delta = normalizeDegrees(b.ra - a.ra + 180.0) - 180.0
        return EquatorialCoordinate(normalizeDegrees(a.ra + delta * fraction), a.dec + (b.dec - a.dec) * fraction)
    }
}
