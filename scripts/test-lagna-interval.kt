package dev.yantra.app.calendar

import dev.yantra.app.engine.*
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

/** Standalone engine regression checks; no Android runtime required. */
fun main() {
    val origin = ZonedDateTime.parse("2026-10-08T23:00:00Z")
    val originJd = AstronomyEngine().compute(origin, Observer(0.0, 0.0)).julianDay
    val provider = object : LongitudeProvider {
        override fun longitudes(julianDayUt: Double, observer: Observer) = EclipticLongitudes(
            0.0, 90.0, normalizeDegrees((julianDayUt - originJd) * 360.0), true,
        )
    }
    val engine = YantraCalendarEngine(AstronomyEngine(provider))
    val observer = Observer(45.5, -73.6)
    fun close(actual: ZonedDateTime, expected: ZonedDateTime) {
        check(abs(Duration.between(actual, expected).seconds) <= 2) { "$actual != $expected" }
    }
    // The active sign starts yesterday and ends today: do not clip at midnight.
    val current = engine.lagnaInterval(origin.plusMinutes(90), observer, 0)!!
    close(current.first, origin)
    close(current.second, origin.plusHours(2))
    // Navigating the shared chart to another sign finds its next passage.
    val next = engine.lagnaInterval(origin.plusMinutes(90), observer, 2)!!
    close(next.first, origin.plusHours(4))
    close(next.second, origin.plusHours(6))
    // References in a different zone must describe the same instants.
    val toronto = origin.plusMinutes(90).withZoneSameInstant(ZoneId.of("America/Toronto"))
    val zoned = engine.lagnaInterval(toronto, observer, 0)!!
    close(zoned.first, current.first)
    close(zoned.second, current.second)
    check(zoned.first.zone == toronto.zone)
    // Unavailable native ascendant data must not create invented timings.
    check(YantraCalendarEngine().lagnaInterval(origin, observer, 0) == null)
    // A sign that never occurs should terminate with no interval.
    val constant = object : LongitudeProvider {
        override fun longitudes(julianDayUt: Double, observer: Observer) =
            EclipticLongitudes(0.0, 90.0, 0.0, true)
    }
    check(YantraCalendarEngine(AstronomyEngine(constant)).lagnaInterval(origin, observer, 1) == null)
    println("Lagna interval checks passed: midnight, next sign, time zone, unavailable data, absent sign.")
}
