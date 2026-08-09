package dev.yantra.app.engine

import dev.yantra.app.calendar.Ayanamsa

class SwissEphemeris(
    private val ephemerisPath: String,
    private val ayanamsa: Ayanamsa = Ayanamsa.Lahiri,
) : LongitudeProvider {
    override fun longitudes(julianDayUt: Double, observer: Observer): EclipticLongitudes? {
        if (!NativeBridge.available) return null
        val values = nativeLongitudes(julianDayUt, observer.latitude, observer.longitude, ephemerisPath, ayanamsa.swissMode)
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
