package dev.yantra.app.engine

class SwissEphemeris(
    private val ephemerisPath: String,
) : LongitudeProvider {
    override fun longitudes(julianDayUt: Double): EclipticLongitudes? {
        if (!NativeBridge.available) return null
        val values = nativeLongitudes(julianDayUt, ephemerisPath)
        val sun = values.getOrNull(0) ?: return null
        val moon = values.getOrNull(1) ?: return null
        if (sun < 0.0 || moon < 0.0) return null
        return EclipticLongitudes(
            solarLongitude = normalizeDegrees(sun),
            lunarLongitude = normalizeDegrees(moon),
            sidereal = true,
        )
    }

    private external fun nativeLongitudes(julianDayUt: Double, ephemerisPath: String): DoubleArray

    private object NativeBridge {
        val available: Boolean = runCatching {
            System.loadLibrary("yantra_ephemeris")
        }.isSuccess
    }
}
