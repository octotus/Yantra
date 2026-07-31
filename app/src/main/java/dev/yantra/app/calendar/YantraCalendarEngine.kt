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
    private val calendarLocaleRule: CalendarLocaleRule = CalendarCatalog.calendarLocaleRules.first(),
    private val monthReckoning: MonthReckoning = MonthReckoning.Amanta,
) {
    fun nakshatraInterval(dateTime: ZonedDateTime, observer: Observer, targetIndex: Int): Pair<ZonedDateTime, ZonedDateTime>? {
        var inside: ZonedDateTime? = null
        for (offsetHours in 0..(24 * 16) step 2) {
            for (direction in listOf(1, -1)) {
                val candidate = dateTime.plusHours(offsetHours.toLong() * direction)
                if (compute(candidate, observer).nakshatra.index == targetIndex) {
                    inside = candidate
                    break
                }
            }
            if (inside != null) break
        }
        val anchor = inside ?: return null
        var before = anchor
        while (compute(before, observer).nakshatra.index == targetIndex) before = before.minusMinutes(30)
        var after = anchor
        while (compute(after, observer).nakshatra.index == targetIndex) after = after.plusMinutes(30)
        return refineNakshatraBoundary(before, before.plusMinutes(30), observer, targetIndex, entering = true) to
            refineNakshatraBoundary(after.minusMinutes(30), after, observer, targetIndex, entering = false)
    }

    private fun refineNakshatraBoundary(
        start: ZonedDateTime,
        end: ZonedDateTime,
        observer: Observer,
        targetIndex: Int,
        entering: Boolean,
    ): ZonedDateTime {
        var low = start
        var high = end
        while (java.time.Duration.between(low, high).toMinutes() > 1) {
            val middle = low.plusSeconds(java.time.Duration.between(low, high).seconds / 2)
            val isTarget = compute(middle, observer).nakshatra.index == targetIndex
            if (isTarget == entering) high = middle else low = middle
        }
        return high
    }
    private companion object {
        private const val SAMVATSARA_YEAR_OFFSET = 53
    }

    private val monthResolutionCache = linkedMapOf<String, MonthResolution>()
    private val lagnaResolutionCache = linkedMapOf<String, LagnaResolution>()

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
        val monthResolution = resolveLunarYearMonths(dateTime, observer, celestial.julianDay)
        val yogaIndex = floor(normalizeDegrees(siderealSun + siderealMoon) / (360.0 / 27.0)).toInt().coerceIn(0, 26)
        val samvatsaraIndex = resolveSamvatsaraIndex(monthResolution)
        val monthSectors = monthResolution.sectors
        val paksha = if (tithiIndex < 15) "Shukla" else "Krishna"
        val activeMonthIndex = when {
            monthReckoning == MonthReckoning.Purnimanta && paksha == "Krishna" ->
                Math.floorMod(monthResolution.activeMonthIndex + 1, 12)
            else -> monthResolution.activeMonthIndex
        }
        val activeMonth = monthSectors[activeMonthIndex]
        val siderealAscendant = celestial.ascendantLongitude?.let { ascendant ->
            if (celestial.longitudesAreSidereal) ascendant else normalizeDegrees(ascendant - ayanamsa)
        }
        val lagnaResolution = resolveDailyLagnaSectors(dateTime, observer)
        val lagnaRashiIndex = siderealAscendant?.let { floor(it / 30.0).toInt().coerceIn(0, 11) }

        return YantraState(
            julianDay = celestial.julianDay,
            solarLongitude = siderealSun,
            lunarLongitude = siderealMoon,
            ascendantLongitude = siderealAscendant,
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
            lagnaSectors = lagnaResolution.sectors,
            lagnaRashi = lagnaRashiIndex?.let { CalendarCatalog.rashis[it] },
            lagnaDayFraction = dayFraction(dateTime),
            samvatsara = CalendarCatalog.samvatsaras[samvatsaraIndex],
            paksha = paksha,
            lunarMonth = activeMonth.name,
            yoga = CalendarCatalog.yogas[yogaIndex],
        )
    }

    private fun resolveDailyLagnaSectors(dateTime: ZonedDateTime, observer: Observer): LagnaResolution {
        val cacheKey = listOf(
            dateTime.toLocalDate().toString(),
            "%.4f".format(observer.latitude),
            "%.4f".format(observer.longitude),
        ).joinToString("|")
        lagnaResolutionCache[cacheKey]?.let { return it }

        val midnight = dateTime.toLocalDate().atStartOfDay(dateTime.zone)
        val samples = mutableListOf<LagnaSample>()
        for (minute in 0..1440 step 5) {
            val sampleTime = midnight.plusMinutes(minute.toLong())
            val sample = astronomyEngine.compute(sampleTime, observer)
            val sampleAyanamsa = lahiriAyanamsaApprox(sample.julianDay)
            val ascendant = sample.ascendantLongitude?.let {
                if (sample.longitudesAreSidereal) it else normalizeDegrees(it - sampleAyanamsa)
            }
            if (ascendant != null) {
                samples += LagnaSample(minute / 1440.0, floor(ascendant / 30.0).toInt().coerceIn(0, 11))
            }
        }
        if (samples.size < 2) return cacheLagnaResolution(cacheKey, equalLagnaResolution())

        val sectors = mutableListOf<LagnaSector>()
        var currentIndex = samples.first().rashiIndex
        var currentStart = 0.0
        for (sampleIndex in 1 until samples.size) {
            val sample = samples[sampleIndex]
            if (sample.rashiIndex != currentIndex) {
                val previous = samples[sampleIndex - 1]
                val boundary = (previous.fraction + sample.fraction) / 2.0
                sectors += lagnaSector(currentIndex, currentStart, boundary)
                currentIndex = sample.rashiIndex
                currentStart = boundary
            }
        }
        sectors += lagnaSector(currentIndex, currentStart, 1.0)

        val normalized = sectors.filter { it.durationFraction > 0.0001 }
        return cacheLagnaResolution(cacheKey, LagnaResolution(normalized.ifEmpty { equalLagnaResolution().sectors }))
    }

    private fun cacheLagnaResolution(key: String, resolution: LagnaResolution): LagnaResolution {
        if (lagnaResolutionCache.size > 8) lagnaResolutionCache.clear()
        lagnaResolutionCache[key] = resolution
        return resolution
    }

    private fun lagnaSector(index: Int, start: Double, end: Double): LagnaSector =
        LagnaSector(
            index = index,
            name = CalendarCatalog.rashis[index].name,
            startFraction = start.coerceIn(0.0, 1.0),
            durationFraction = (end - start).coerceAtLeast(0.0),
        )

    private fun equalLagnaResolution(): LagnaResolution =
        LagnaResolution(
            CalendarCatalog.rashis.mapIndexed { index, rashi ->
                LagnaSector(index, rashi.name, index / 12.0, 1.0 / 12.0)
            }
        )

    private fun dayFraction(dateTime: ZonedDateTime): Double =
        (dateTime.toLocalTime().toSecondOfDay() + dateTime.nano / 1_000_000_000.0) / 86_400.0

    private fun resolveLunarYearMonths(
        dateTime: ZonedDateTime,
        observer: Observer,
        julianDay: Double,
    ): MonthResolution {
        val cacheKey = listOf(dateTime.toLocalDate().toString(), calendarLocaleRule.id, monthReckoning.id).joinToString("|")
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
        val newYearMonthIndex = calendarLocaleRule.newYearMonthIndex.coerceIn(0, 11)
        val yearStart = labeled.withIndex().filter { it.index <= activeIndex && it.value.monthIndex == newYearMonthIndex }.maxByOrNull { it.index }?.index
            ?: labeled.withIndex().filter { it.index <= activeIndex }.maxByOrNull { it.index }?.index
            ?: return cacheMonthResolution(cacheKey, fallbackMonthResolution(dateTime, observer))
        val yearEnd = labeled.withIndex().firstOrNull { it.index > yearStart && it.value.monthIndex == newYearMonthIndex }?.index
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
        val yearStartDateTime = dateTimeAtJulianDay(dateTime, julianDay, labeled[yearStart].lunation.start)
        return cacheMonthResolution(
            cacheKey,
            MonthResolution(
                sectors = sectors,
                activeMonthIndex = labeled[activeIndex].monthIndex,
                lunisolarYearStartYear = yearStartDateTime.year,
            )
        )
    }

    private fun resolveSamvatsaraIndex(monthResolution: MonthResolution): Int =
        Math.floorMod(monthResolution.lunisolarYearStartYear + SAMVATSARA_YEAR_OFFSET, 60)

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
        val newYearMonthIndex = calendarLocaleRule.newYearMonthIndex.coerceIn(0, 11)
        val lunisolarYearStartYear = if (monthIndex < newYearMonthIndex) dateTime.year - 1 else dateTime.year
        return MonthResolution(sectors, monthIndex, lunisolarYearStartYear)
    }

    private fun lahiriAyanamsaApprox(julianDay: Double): Double {
        val yearsSinceJ2000 = (julianDay - 2451545.0) / 365.2425
        return 23.853055 + (50.290966 / 3600.0) * yearsSinceJ2000
    }

    private data class MonthResolution(
        val sectors: List<MonthSector>,
        val activeMonthIndex: Int,
        val lunisolarYearStartYear: Int,
    )

    private data class LagnaResolution(
        val sectors: List<LagnaSector>,
    )

    private data class LagnaSample(
        val fraction: Double,
        val rashiIndex: Int,
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
