package dev.yantra.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.yantra.app.calendar.YantraCalendarEngine
import dev.yantra.app.engine.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val DAY_SECTION_PREFS = "yantra_day_sections"

private enum class DaySection(val label: String) {
    Festivals("Festivals"),
    Special("Special Days"),
    User("User Days");

    fun next(): DaySection = entries[(ordinal + 1) % entries.size]
}

private data class DayOccurrence(val at: ZonedDateTime)

private data class DayBrowserItem(
    val key: String,
    val name: String,
    val defaultSection: DaySection,
    val occurrences: List<DayOccurrence>,
    val recurring: Boolean = false,
)

private data class RecurringTithi(val name: String, val indexes: List<Int>)

private val recurringTithis = listOf(
    RecurringTithi("Amavasya", listOf(29)),
    RecurringTithi("Purnima", listOf(14)),
    RecurringTithi("Ekadashi", listOf(10, 25)),
    RecurringTithi("Pradosham", listOf(12, 27)),
)

@Composable
internal fun DaysScreen(
    context: Context,
    engine: YantraCalendarEngine,
    observer: Observer,
    now: ZonedDateTime,
    todayFestival: FestivalDefinition?,
    specialDays: List<SpecialDay>,
    userEvents: List<UserEvent>,
    onDismiss: () -> Unit,
    onSelect: (ZonedDateTime) -> Unit,
    onAddDays: () -> Unit,
) {
    val ivory = Color(0xFFFFE8B0)
    val gold = Color(0xFFE8CA8B)
    val brightGold = Color(0xFFFFE2A3)
    val copper = Color(0xFF8E5424)
    val deepCopper = Color(0xFF211007)
    var items by remember { mutableStateOf<List<DayBrowserItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(false) }
    val openSections = remember { mutableStateMapOf<DaySection, Boolean>() }
    val openRecurring = remember { mutableStateMapOf<String, Boolean>() }
    val overrides = remember { mutableStateMapOf<String, DaySection>().apply { putAll(loadDaySections(context)) } }

    LaunchedEffect(engine, observer, now.toLocalDate(), specialDays, userEvents) {
        loading = true
        loadError = null
        val result = withContext(Dispatchers.Default) { runCatching { buildDayItems(engine, observer, now, specialDays, userEvents) } }
        items = result.getOrElse { emptyList() }
        loadError = result.exceptionOrNull()?.message
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(0f to Color(0xFF3A1D0C), 0.55f to Color(0xFF160B05), 1f to Color(0xFF050302)))
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("DAYS", color = ivory, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = brightGold)) { Text("Back") }
            }
            todayFestival?.let {
                Text("TODAY", color = gold, style = MaterialTheme.typography.labelLarge)
                Text(it.name, color = ivory, style = MaterialTheme.typography.titleMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddDays, colors = ButtonDefaults.buttonColors(containerColor = copper, contentColor = ivory)) { Text("Add days") }
                TextButton(onClick = { editMode = !editMode }, colors = ButtonDefaults.textButtonColors(contentColor = brightGold)) {
                    Text(if (editMode) "Done" else "Reclassify")
                }
            }
            if (loading) {
                CircularProgressIndicator(color = gold, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (loadError != null) {
                Text("Days could not be calculated. Please close this screen and try again.", color = ivory)
            } else {
                DaySection.entries.forEach { section ->
                    val sectionItems = items.filter { (overrides[it.key] ?: it.defaultSection) == section }
                    val open = openSections[section] == true
                    Button(
                        onClick = { openSections[section] = !open },
                        colors = ButtonDefaults.buttonColors(containerColor = deepCopper, contentColor = ivory),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("${if (open) "▾" else "▸"} ${section.label} (${sectionItems.size})") }
                    if (open) sectionItems.forEach { item ->
                        DayItemRow(
                            item = item,
                            editMode = editMode,
                            expanded = openRecurring[item.key] == true,
                            section = overrides[item.key] ?: item.defaultSection,
                            gold = gold,
                            ivory = ivory,
                            onClick = {
                                if (item.recurring) openRecurring[item.key] = !(openRecurring[item.key] == true)
                                else item.occurrences.firstOrNull()?.let { onSelect(it.at) }
                            },
                            onOccurrence = { onSelect(it.at) },
                            onReclassify = {
                                val next = (overrides[item.key] ?: item.defaultSection).next()
                                overrides[item.key] = next
                                saveDaySections(context, overrides)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayItemRow(
    item: DayBrowserItem,
    editMode: Boolean,
    expanded: Boolean,
    section: DaySection,
    gold: Color,
    ivory: Color,
    onClick: () -> Unit,
    onOccurrence: (DayOccurrence) -> Unit,
    onReclassify: () -> Unit,
) {
    val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
    Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = ivory)) {
                val suffix = if (item.recurring) "  ${if (expanded) "▾" else "▸"}" else item.occurrences.firstOrNull()?.let { "  ${it.at.format(dateFormat)}" }.orEmpty()
                Text(item.name + suffix)
            }
            if (editMode) TextButton(onClick = onReclassify, colors = ButtonDefaults.textButtonColors(contentColor = gold)) { Text("Move from ${section.label}") }
        }
        if (expanded) item.occurrences.forEach { occurrence ->
            TextButton(onClick = { onOccurrence(occurrence) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = gold)) {
                Text(occurrence.at.format(dateFormat))
            }
        }
    }
}

private fun buildDayItems(
    engine: YantraCalendarEngine,
    observer: Observer,
    now: ZonedDateTime,
    specialDays: List<SpecialDay>,
    userEvents: List<UserEvent>,
): List<DayBrowserItem> {
    val end = now.plusMonths(12)
    val result = mutableListOf<DayBrowserItem>()
    val tithiCache = mutableMapOf<Int, List<DayOccurrence>>()
    val nakshatraCache = mutableMapOf<Int, List<DayOccurrence>>()
    val solarRashiCache = mutableMapOf<Int, List<DayOccurrence>>()
    val lunarRashiCache = mutableMapOf<Int, List<DayOccurrence>>()

    fun collectIntervals(intervalAt: (ZonedDateTime) -> Pair<ZonedDateTime, ZonedDateTime>?): List<DayOccurrence> {
        val found = mutableListOf<DayOccurrence>()
        var cursor = now
        while (cursor.isBefore(end)) {
            val interval = intervalAt(cursor) ?: break
            if (interval.first.isAfter(end)) break
            if (interval.second.isAfter(now)) found += DayOccurrence(interval.first)
            cursor = interval.second.plusMinutes(2)
        }
        return found.distinctBy { it.at.toLocalDate() }.sortedBy { it.at }
    }

    fun tithiOccurrences(index: Int) = tithiCache.getOrPut(index) {
        collectIntervals { engine.tithiInterval(it, observer, index) }
    }
    fun nakshatraOccurrences(index: Int) = nakshatraCache.getOrPut(index) {
        collectIntervals { engine.nakshatraInterval(it, observer, index) }
    }
    fun rashiOccurrences(index: Int, solar: Boolean) = (if (solar) solarRashiCache else lunarRashiCache).getOrPut(index) {
        collectIntervals { engine.rashiInterval(it, observer, index, solar) }
    }
    fun tithiIndexes(number: Int, paksha: String?): List<Int> = when (paksha) {
        "Shukla" -> listOf(number - 1)
        "Krishna" -> listOf(number + 14)
        else -> listOf(number - 1, number + 14)
    }
    fun firstMatching(candidates: List<DayOccurrence>, predicate: (dev.yantra.app.calendar.YantraState, dev.yantra.app.calendar.YantraState) -> Boolean): DayOccurrence? {
        return candidates.sortedBy { it.at }.firstOrNull { occurrence ->
            val sample = occurrence.at.plusMinutes(2)
            val state = engine.compute(sample, observer, includeLagna = false)
            val previous = engine.compute(sample.minusDays(1), observer, includeLagna = false)
            predicate(state, previous)
        }
    }

    recurringTithis.forEach { recurring ->
        val occurrences = recurring.indexes.flatMap(::tithiOccurrences).distinctBy { it.at.toLocalDate() }.sortedBy { it.at }
        result += DayBrowserItem("recurring:${recurring.name}", recurring.name, DaySection.Special, occurrences, recurring = true)
    }
    val annualFestivals = FestivalCatalog.definitions.filterNot { it.month == null && it.solarRashi == null && it.tithiNumber in listOf(11, 13) }
    annualFestivals.forEach { festival ->
        val candidates = when {
            festival.tithiNumber != null -> tithiIndexes(festival.tithiNumber, festival.paksha).flatMap(::tithiOccurrences)
            festival.nakshatra != null -> dev.yantra.app.calendar.CalendarCatalog.nakshatras.indexOfFirst { it.name == festival.nakshatra }.takeIf { it >= 0 }?.let(::nakshatraOccurrences).orEmpty()
            festival.solarRashi != null -> dev.yantra.app.calendar.CalendarCatalog.rashis.indexOfFirst { it.name == festival.solarRashi }.takeIf { it >= 0 }?.let { rashiOccurrences(it, solar = true) }.orEmpty()
            else -> emptyList()
        }
        firstMatching(candidates) { state, previous -> FestivalCatalog.matches(festival, state, previous) }?.let {
            result += DayBrowserItem("festival:${festival.name}", festival.name, DaySection.Festivals, listOf(it))
        }
    }
    specialDays.forEach { day ->
        val key = "${day.name}:${day.month}:${day.paksha}:${day.tithiNumber}"
        val candidates = tithiIndexes(day.tithiNumber, day.paksha).flatMap(::tithiOccurrences)
        firstMatching(candidates) { state, _ -> day.matches(state) }?.let {
            result += DayBrowserItem("special:$key", day.name, DaySection.Special, listOf(it))
        }
    }
    userEvents.forEach { event ->
        val candidates = when {
            event.tithiIndex != null -> tithiOccurrences(event.tithiIndex)
            event.nakshatra != null -> dev.yantra.app.calendar.CalendarCatalog.nakshatras.indexOfFirst { it.name == event.nakshatra }.takeIf { it >= 0 }?.let(::nakshatraOccurrences).orEmpty()
            event.rashi != null -> dev.yantra.app.calendar.CalendarCatalog.rashis.indexOfFirst { it.name == event.rashi }.takeIf { it >= 0 }?.let { index -> rashiOccurrences(index, true) + rashiOccurrences(index, false) }.orEmpty()
            event.paksha == "Shukla" -> tithiOccurrences(0)
            event.paksha == "Krishna" -> tithiOccurrences(15)
            else -> tithiOccurrences(0) + tithiOccurrences(15)
        }
        firstMatching(candidates) { state, _ -> event.matches(state) }?.let {
            result += DayBrowserItem("user:${event.identityKey()}", event.name, DaySection.User, listOf(it))
        }
    }
    return result.sortedBy { it.name }
}

private fun loadDaySections(context: Context): Map<String, DaySection> =
    context.getSharedPreferences(DAY_SECTION_PREFS, Context.MODE_PRIVATE).all.mapNotNull { (key, value) ->
        DaySection.entries.firstOrNull { it.name == value }?.let { key to it }
    }.toMap()

private fun saveDaySections(context: Context, values: Map<String, DaySection>) {
    context.getSharedPreferences(DAY_SECTION_PREFS, Context.MODE_PRIVATE).edit().clear().apply {
        values.forEach { (key, section) -> putString(key, section.name) }
    }.apply()
}
