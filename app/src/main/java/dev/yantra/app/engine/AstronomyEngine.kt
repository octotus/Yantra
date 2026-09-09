package dev.yantra.app.engine

import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

data class Observer(
    val latitude: Double,
    val longitude: Double,
)

data class CelestialState(
    val julianDay: Double,
    val solarLongitude: Double,
    val lunarLongitude: Double,
    val ascendantLongitude: Double?,
    val longitudesAreSidereal: Boolean,
    val solarAltitude: Double,
    val lunarAltitude: Double,
    val moonIllumination: Double,
)

data class EclipticLongitudes(
    val solarLongitude: Double,
    val lunarLongitude: Double,
    val ascendantLongitude: Double?,
    val sidereal: Boolean,
)

interface LongitudeProvider {
    fun longitudes(julianDayUt: Double, observer: Observer): EclipticLongitudes?
    fun chartSnapshot(julianDayUt: Double): CelestialSnapshot? = null
}

class AstronomyEngine(
    private val longitudeProvider: LongitudeProvider? = null,
) {
    fun chartSnapshot(dateTime: ZonedDateTime): CelestialSnapshot? =
        longitudeProvider?.chartSnapshot(julianDay(dateTime))

    fun compute(dateTime: ZonedDateTime, observer: Observer): CelestialState {
        val jd = julianDay(dateTime)
        val d = jd - 2451545.0
        val approximateSunLongitude = normalizeDegrees(
            280.460 + 0.9856474 * d +
                1.915 * sinDeg(357.528 + 0.9856003 * d) +
                0.020 * sinDeg(2.0 * (357.528 + 0.9856003 * d))
        )

        val approximateMoonLongitude = approximateMoonLongitude(d)
        val precise = longitudeProvider?.longitudes(jd, observer)
        val sunLongitude = precise?.solarLongitude ?: approximateSunLongitude
        val moonLongitude = precise?.lunarLongitude ?: approximateMoonLongitude
        val solarAltitude = altitude(dateTime, observer, approximateSunLongitude, 0.0)
        val lunarAltitude = altitude(dateTime, observer, approximateMoonLongitude, 5.14 * sinDeg(d * 13.176396))
        val elongation = normalizeDegrees(approximateMoonLongitude - approximateSunLongitude)
        val illumination = (1.0 - cos(toRadians(elongation))) / 2.0

        return CelestialState(
            julianDay = jd,
            solarLongitude = sunLongitude,
            lunarLongitude = moonLongitude,
            ascendantLongitude = precise?.ascendantLongitude,
            longitudesAreSidereal = precise?.sidereal ?: false,
            solarAltitude = solarAltitude,
            lunarAltitude = lunarAltitude,
            moonIllumination = illumination.coerceIn(0.0, 1.0),
        )
    }

    private fun julianDay(dateTime: ZonedDateTime): Double {
        val utc = dateTime.withZoneSameInstant(java.time.ZoneOffset.UTC)
        var y = utc.year
        var m = utc.monthValue
        val dayFraction = (utc.dayOfMonth +
            (utc.hour + (utc.minute + (utc.second + utc.nano / 1_000_000_000.0) / 60.0) / 60.0) / 24.0)

        if (m <= 2) {
            y -= 1
            m += 12
        }

        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) +
            floor(30.6001 * (m + 1)) +
            dayFraction + b - 1524.5
    }

    private fun approximateMoonLongitude(daysSinceJ2000: Double): Double {
        val l0 = normalizeDegrees(218.316 + 13.176396 * daysSinceJ2000)
        val mMoon = normalizeDegrees(134.963 + 13.064993 * daysSinceJ2000)
        val mSun = normalizeDegrees(357.529 + 0.98560028 * daysSinceJ2000)
        val d = normalizeDegrees(297.850 + 12.190749 * daysSinceJ2000)
        val f = normalizeDegrees(93.272 + 13.229350 * daysSinceJ2000)

        return normalizeDegrees(
            l0 +
                6.289 * sinDeg(mMoon) +
                1.274 * sinDeg(2 * d - mMoon) +
                0.658 * sinDeg(2 * d) +
                0.214 * sinDeg(2 * mMoon) -
                0.186 * sinDeg(mSun) -
                0.114 * sinDeg(2 * f)
        )
    }

    private fun altitude(
        dateTime: ZonedDateTime,
        observer: Observer,
        longitude: Double,
        declinationHint: Double,
    ): Double {
        val jd = julianDay(dateTime)
        val centuries = (jd - 2451545.0) / 36525.0
        val obliquity = 23.439291 - 0.0130042 * centuries
        val declination = asin(sinDeg(declinationHint) * cosDeg(obliquity) + cosDeg(declinationHint) * sinDeg(obliquity) * sinDeg(longitude))
        val rightAscension = atan2(cosDeg(obliquity) * sinDeg(longitude), cosDeg(longitude))
        val sidereal = normalizeDegrees(280.46061837 + 360.98564736629 * (jd - 2451545.0) + observer.longitude)
        val hourAngle = normalizeDegrees(sidereal - Math.toDegrees(rightAscension))
        val adjustedHourAngle = if (hourAngle > 180.0) hourAngle - 360.0 else hourAngle
        val lat = toRadians(observer.latitude)
        val altitude = asin(sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(toRadians(adjustedHourAngle)))
        return Math.toDegrees(altitude)
    }
}

fun normalizeDegrees(value: Double): Double {
    val normalized = value % 360.0
    return if (normalized < 0) normalized + 360.0 else normalized
}

fun toRadians(value: Double): Double = value * PI / 180.0

fun sinDeg(value: Double): Double = sin(toRadians(value))

fun cosDeg(value: Double): Double = cos(toRadians(value))
