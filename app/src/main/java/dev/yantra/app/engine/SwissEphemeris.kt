package dev.yantra.app.engine

import dev.yantra.app.calendar.Ayanamsa

class SwissEphemeris(
    private val ephemerisPath: String,
    private val ayanamsa: Ayanamsa = Ayanamsa.Lahiri,
) : LongitudeProvider {
    override fun longitudes(julianDayUt: Double, observer: Observer): EclipticLongitudes? {
        if (!NativeBridge.available) return null
        val values = synchronized(NativeBridge) {
            nativeLongitudes(julianDayUt, observer.latitude, observer.longitude, ephemerisPath, ayanamsa.swissMode)
        }
        val sun = values.getOrNull(0) ?: return null
        val moon = values.getOrNull(1) ?: return null
        val ascendant = values.getOrNull(4)
        if (sun < 0.0 || moon < 0.0) return null
        return EclipticLongitudes(
            solarLongitude = normalizeDegrees(sun),
            lunarLongitude = normalizeDegrees(moon),
            ascendantLongitude = ascendant?.takeIf { it >= 0.0 }?.let { normalizeDegrees(it) },
            sidereal = true,
        )
    }

    override fun chartSnapshot(julianDayUt: Double): CelestialSnapshot? {
        if (!NativeBridge.available) return null
        val values = synchronized(NativeBridge) {
            nativeChart(julianDayUt, ephemerisPath, ayanamsa.swissMode)
        }
        if (values.size != 729 || values.any { !it.isFinite() }) return null
        return CelestialSnapshot(
            EquatorialCoordinate(values[0], values[1]),
            EquatorialCoordinate(values[2], values[3]),
            values[4].coerceIn(0.0, 1.0), values[5], values[6],
            (0..360).map { EquatorialCoordinate(values[7 + it * 2], values[8 + it * 2]) },
        )
    }

    private external fun nativeChart(julianDayUt: Double, ephemerisPath: String, ayanamsaMode: Int): DoubleArray

    private external fun nativeLongitudes(
        julianDayUt: Double,
        latitude: Double,
        longitude: Double,
        ephemerisPath: String,
        ayanamsaMode: Int,
    ): DoubleArray

    private object NativeBridge {
        val available: Boolean = runCatching {
            System.loadLibrary("yantra_ephemeris")
        }.isSuccess
    }
}
