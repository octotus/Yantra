package dev.yantra.app.calendar

import dev.yantra.app.engine.AstronomyEngine
import dev.yantra.app.engine.Observer
import dev.yantra.app.engine.normalizeDegrees
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.floor

class YantraCalendarEngine(
    private val astronomyEngine: AstronomyEngine = AstronomyEngine(),
) {
    private val monthResolutionCache = linkedMapOf<String, MonthResolution>()

    fun current(observer: Observer): YantraState = compute(ZonedDateTime.now(), observer)

    fun compute(dateTime: ZonedDateTime, observer: Observer): YantraState {
        val celestial = astronomyEngine.compute(dateTime, observer)
        val ayanamsa = lahiriAyanamsaApprox(celestial.julianDay)
        val siderealSun = if (celestial.longitudesAreSidereal) {
            celestial.solarLongitude
        } else {
            normalizeDegrees(celestial.solarLongitude - ayanamsa)
        }
        val siderealMoon = if (celestial.longitudesAreSidereal) {
            celestial.lunarLongitude
        } else {
            normalizeDegrees(celestial.lunarLongitude - ayanamsa)
        }
        val lunarSolarArc = normalizeDegrees(siderealMoon - siderealSun)
        val tithiIndex = floor(lunarSolarArc / 12.0).toInt().coerceIn(0, 29)
        val nakshatraIndex = floor(siderealMoon / (360.0 / 27.0)).toInt().coerceIn(0, 26)
        val solarRashiIndex = floor(siderealSun / 30.0).toInt().coerceIn(0, 11)
        val lunarRashiIndex = floor(siderealMoon / 30.0).toInt().coerceIn(0, 11)
        val yogaIndex = floor(normalizeDegrees(siderealSun + siderealMoon) / (360.0 / 27.0)).toInt().coerceIn(0, 26)
        val samvatsaraIndex = Math.floorMod(dateTime.year + 56, 60)
        val monthResolution = resolveLunarYearMonths(dateTime, observer, celestial.julianDay)
        val monthSectors = monthResolution.sectors
        val activeMonth = monthSectors[monthResolution.activeMonthIndex]

        return YantraState(
            julianDay = celestial.julianDay,
            solarLongitude = siderealSun,
            lunarLongitude = siderealMoon,
            solarAltitude = celestial.solarAltitude,
            lunarAltitude = celestial.lunarAltitude,
            moonIllumination = celestial.moonIllumination,
            tithi = CalendarCatalog.tithis[tithiIndex],
            nakshatra = CalendarCatalog.nakshatras[nakshatraIndex],
            rashi = CalendarCatalog.rashis[solarRashiIndex],
            solarRashi = CalendarCatalog.rashis[solarRashiIndex],
            lunarRashi = CalendarCatalog.rashis[lunarRashiIndex],
            month = activeMonth,
            monthSectors = monthSectors,
            samvatsara = CalendarCatalog.samvatsaras[samvatsaraIndex],
            paksha = if (tithiIndex < 15) "Shukla" else "Krishna",
            lunarMonth = activeMonth.name,
            yoga = CalendarCatalog.yogas[yogaIndex],
        )
    }

    private fun resolveLunarYearMonths(
        dateTime: ZonedDateTime,
        observer: Observer,
        julianDay: Double,
    ): MonthResolution {
        val cacheKey = dateTime.toLocalDate().toString()
        monthResolutionCache[cacheKey]?.let { return it }

        val newMoons = findNewMoons(julianDay - 430.0, julianDay + 430.0, dateTime, observer, julianDay)
        if (newMoons.size < 16) return cacheMonthResolution(cacheKey, fallbackMonthResolution(dateTime, observer))

        val lunations = newMoons.zipWithNext().map { (start, end) ->
            Lunation(
                start = start,
                end = end,
                sankrantis = findSankrantis(start, end, dateTime, observer, julianDay),
            )
        }
        val labeled = lunations.mapIndexed { index, lunation ->
            LabeledLunation(lunation, resolveMonthIndex(lunations, index))
        }
        val activeIndex = labeled.indexOfLast { julianDay >= it.lunation.start && julianDay < it.lunation.end }
            .takeIf { it >= 0 } ?: return cacheMonthResolution(cacheKey, fallbackMonthResolution(dateTime, observer))
        val yearStart = labeled.withIndex().filter { it.index <= activeIndex && it.value.monthIndex == 0 }.maxByOrNull { it.index }?.index
            ?: labeled.withIndex().filter { it.index <= activeIndex }.maxByOrNull { it.index }?.index
            ?: return cacheMonthResolution(cacheKey, fallbackMonthResolution(dateTime, observer))
        val yearEnd = labeled.withIndex().firstOrNull { it.index > yearStart && it.value.monthIndex == 0 }?.index
            ?: (yearStart + 12).coerceAtMost(labeled.size)
        if (yearEnd <= yearStart) return cacheMonthResolution(cacheKey, fallbackMonthResolution(dateTime, observer))

        val durations = DoubleArray(12)
        for (index in yearStart until yearEnd) {
            val labeledLunation = labeled[index]
            val lunation = labeledLunation.lunation
            when (lunation.sankrantis.size) {
                0, 1 -> durations[labeledLunation.monthIndex] += lunation.end - lunation.start
                else -> {
                    val first = lunation.sankrantis[0]
                    durations[first.rashiIndex] += (first.julianDay - lunation.start).coerceAtLeast(0.0)
                    for (sankrantiIndex in 1 until lunation.sankrantis.size) {
                        val previous = lunation.sankrantis[sankrantiIndex - 1]
                        val current = lunation.sankrantis[sankrantiIndex]
                        durations[current.rashiIndex] += (current.julianDay - previous.julianDay).coerceAtLeast(0.0)
                    }
                    val last = lunation.sankrantis.last()
                    durations[last.rashiIndex] += (lunation.end - last.julianDay).coerceAtLeast(0.0)
                }
            }
        }

        val fallbackDuration = CalendarCatalog.lunarMonths.first().durationDays
        for (index in durations.indices) {
            if (durations[index] <= 0.0) durations[index] = fallbackDuration * 0.35
        }
        val totalDays = durations.sum().takeIf { it > 0.0 } ?: return cacheMonthResolution(cacheKey, fallbackMonthResolution(dateTime, observer))
        val sectors = CalendarCatalog.lunarMonths.mapIndexed { index, month ->
            month.copy(
                durationDays = durations[index],
                arcDegrees = durations[index] / totalDays * 360.0,
            )
        }
        return cacheMonthResolution(cacheKey, MonthResolution(sectors, labeled[activeIndex].monthIndex))
    }

    private fun cacheMonthResolution(key: String, resolution: MonthResolution): MonthResolution {
        if (monthResolutionCache.size > 16) monthResolutionCache.clear()
        monthResolutionCache[key] = resolution
        return resolution
    }

    private fun resolveMonthIndex(lunations: List<Lunation>, index: Int): Int {
        lunations[index].sankrantis.firstOrNull()?.let { return it.rashiIndex }
        for (nextIndex in index + 1 until lunations.size) {
            lunations[nextIndex].sankrantis.firstOrNull()?.let { return it.rashiIndex }
        }
        for (previousIndex in index - 1 downTo 0) {
            lunations[previousIndex].sankrantis.firstOrNull()?.let { return Math.floorMod(it.rashiIndex + 1, 12) }
        }
        return 0
    }

    private fun findNewMoons(
        fromJulianDay: Double,
        toJulianDay: Double,
        referenceDateTime: ZonedDateTime,
        observer: Observer,
        referenceJulianDay: Double,
    ): List<Double> {
        val newMoons = mutableListOf<Double>()
        var previousJd = fromJulianDay
        var previous = signedElongation(previousJd, referenceDateTime, observer, referenceJulianDay)
        var jd = fromJulianDay + 1.0
        while (jd <= toJulianDay) {
            val current = signedElongation(jd, referenceDateTime, observer, referenceJulianDay)
            if (previous <= 0.0 && current > 0.0 && abs(current - previous) < 80.0) {
                newMoons += bisectCrossing(previousJd, jd) { candidate ->
                    signedElongation(candidate, referenceDateTime, observer, referenceJulianDay)
                }
            }
            previousJd = jd
            previous = current
            jd += 1.0
        }
        return newMoons
    }

    private fun findSankrantis(
        fromJulianDay: Double,
        toJulianDay: Double,
        referenceDateTime: ZonedDateTime,
        observer: Observer,
        referenceJulianDay: Double,
    ): List<Sankranti> {
        val sankrantis = mutableListOf<Sankranti>()
        var previousJd = fromJulianDay
        var previousSun = siderealLongitudes(previousJd, referenceDateTime, observer, referenceJulianDay).sun
        var previousRashi = floor(previousSun / 30.0).toInt().coerceIn(0, 11)
        var jd = fromJulianDay + 0.75
        while (jd <= toJulianDay) {
            val currentSun = siderealLongitudes(jd, referenceDateTime, observer, referenceJulianDay).sun
            val currentRashi = floor(currentSun / 30.0).toInt().coerceIn(0, 11)
            if (currentRashi != previousRashi) {
                val enteredRashi = currentRashi
                val target = if (enteredRashi == 0 && previousRashi == 11) 360.0 else enteredRashi * 30.0
                val startSun = previousSun
                val ingress = bisectCrossing(previousJd, jd) { candidate ->
                    val sun = unwrapLongitude(siderealLongitudes(candidate, referenceDateTime, observer, referenceJulianDay).sun, startSun)
                    sun - target
                }
                sankrantis += Sankranti(ingress, enteredRashi)
            }
            previousJd = jd
            previousSun = currentSun
            previousRashi = currentRashi
            jd += 0.75
        }
        return sankrantis
    }

    private fun bisectCrossing(
        lowerStart: Double,
        upperStart: Double,
        valueAt: (Double) -> Double,
    ): Double {
        var lower = lowerStart
        var upper = upperStart
        var lowerValue = valueAt(lower)
        repeat(36) {
            val mid = (lower + upper) / 2.0
            val midValue = valueAt(mid)
            if ((lowerValue <= 0.0 && midValue <= 0.0) || (lowerValue > 0.0 && midValue > 0.0)) {
                lower = mid
                lowerValue = midValue
            } else {
                upper = mid
            }
        }
        return (lower + upper) / 2.0
    }

    private fun signedElongation(
        julianDay: Double,
        referenceDateTime: ZonedDateTime,
        observer: Observer,
        referenceJulianDay: Double,
    ): Double {
        val longitudes = siderealLongitudes(julianDay, referenceDateTime, observer, referenceJulianDay)
        val arc = normalizeDegrees(longitudes.moon - longitudes.sun)
        return if (arc > 180.0) arc - 360.0 else arc
    }

    private fun siderealLongitudes(
        julianDay: Double,
        referenceDateTime: ZonedDateTime,
        observer: Observer,
        referenceJulianDay: Double,
    ): SiderealLongitudes {
        val dateTime = dateTimeAtJulianDay(referenceDateTime, referenceJulianDay, julianDay)
        val celestial = astronomyEngine.compute(dateTime, observer)
        val ayanamsa = lahiriAyanamsaApprox(celestial.julianDay)
        val sun = if (celestial.longitudesAreSidereal) celestial.solarLongitude else normalizeDegrees(celestial.solarLongitude - ayanamsa)
        val moon = if (celestial.longitudesAreSidereal) celestial.lunarLongitude else normalizeDegrees(celestial.lunarLongitude - ayanamsa)
        return SiderealLongitudes(sun, moon)
    }

    private fun dateTimeAtJulianDay(
        referenceDateTime: ZonedDateTime,
        referenceJulianDay: Double,
        julianDay: Double,
    ): ZonedDateTime {
        val seconds = ((julianDay - referenceJulianDay) * 86_400.0).toLong()
        return referenceDateTime.plus(Duration.ofSeconds(seconds))
    }

    private fun unwrapLongitude(longitude: Double, baseline: Double): Double =
        if (longitude + 180.0 < baseline) longitude + 360.0 else longitude

    private fun fallbackMonthResolution(dateTime: ZonedDateTime, observer: Observer): MonthResolution {
        val celestial = astronomyEngine.compute(dateTime, observer)
        val ayanamsa = lahiriAyanamsaApprox(celestial.julianDay)
        val siderealSun = if (celestial.longitudesAreSidereal) celestial.solarLongitude else normalizeDegrees(celestial.solarLongitude - ayanamsa)
        val monthIndex = floor(siderealSun / 30.0).toInt().coerceIn(0, 11)
        val totalDays = CalendarCatalog.lunarMonths.sumOf { it.durationDays }
        val sectors = CalendarCatalog.lunarMonths.map { month ->
            month.copy(arcDegrees = month.durationDays / totalDays * 360.0)
        }
        return MonthResolution(sectors, monthIndex)
    }

    private fun lahiriAyanamsaApprox(julianDay: Double): Double {
        val yearsSinceJ2000 = (julianDay - 2451545.0) / 365.2425
        return 23.853055 + (50.290966 / 3600.0) * yearsSinceJ2000
    }

    private data class MonthResolution(
        val sectors: List<MonthSector>,
        val activeMonthIndex: Int,
    )

    private data class Lunation(
        val start: Double,
        val end: Double,
        val sankrantis: List<Sankranti>,
    )

    private data class LabeledLunation(
        val lunation: Lunation,
        val monthIndex: Int,
    )

    private data class Sankranti(
        val julianDay: Double,
        val rashiIndex: Int,
    )

    private data class SiderealLongitudes(
        val sun: Double,
        val moon: Double,
    )
}
