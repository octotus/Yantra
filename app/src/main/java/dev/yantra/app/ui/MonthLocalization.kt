package dev.yantra.app.ui

import dev.yantra.app.calendar.CalendarCatalog
import dev.yantra.app.calendar.MonthNameSet
import dev.yantra.app.calendar.MonthSector

internal fun localizedMonthSectors(sectors: List<MonthSector>, monthNameSet: MonthNameSet): List<MonthSector> =
    sectors.map { sector ->
        val index = sector.index.coerceIn(0, CalendarCatalog.lunarMonths.lastIndex)
        sector.copy(
            name = monthNameSet.monthNames.getOrElse(index) { sector.name },
            abbreviation = monthNameSet.abbreviations.getOrElse(index) { sector.abbreviation },
        )
    }

internal fun localizedMonthName(index: Int, monthNameSet: MonthNameSet): String =
    monthNameSet.monthNames.getOrElse(index.coerceIn(0, CalendarCatalog.lunarMonths.lastIndex)) {
        CalendarCatalog.lunarMonths[index.coerceIn(0, CalendarCatalog.lunarMonths.lastIndex)].name
    }

internal fun localizedMonthName(canonicalName: String, monthNameSet: MonthNameSet): String {
    val index = CalendarCatalog.lunarMonths.indexOfFirst { it.name == canonicalName }
    return if (index >= 0) localizedMonthName(index, monthNameSet) else canonicalName
}
