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
import dev.yantra.app.calendar.ObservanceState
import dev.yantra.app.engine.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val DAY_SECTION_PREFS = "yantra_day_sections"
private const val DAY_CACHE_PREFS = "yantra_day_cache"
private const val DAY_CACHE_VERSION = "2"

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
    calendarConfigKey: String,
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

    LaunchedEffect(engine, observer, now.toLocalDate(), specialDays, userEvents, calendarConfigKey) {
        loading = true
        loadError = null
        val cacheKey = dayCacheKey(observer, now, calendarConfigKey, specialDays, userEvents)
        loadCachedDayItems(context, cacheKey)?.let { cached ->
            items = cached
            loading = false
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.Default) { runCatching { buildDayItems(engine, observer, now, specialDays, userEvents) } }
        items = result.getOrElse { emptyList() }
        loadError = result.exceptionOrNull()?.message
        if (result.isSuccess) saveCachedDayItems(context, cacheKey, items)
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
    fun firstMatching(candidates: List<DayOccurrence>, predicate: (ObservanceState, ObservanceState) -> Boolean): DayOccurrence? {
        return candidates.sortedBy { it.at }.firstOrNull { occurrence ->
            val sample = occurrence.at.plusMinutes(2)
            val state = engine.observanceState(sample, observer)
            val previous = engine.observanceState(sample.minusDays(1), observer)
            predicate(state, previous)
        }
    }
    fun festivalMatches(festival: FestivalDefinition, state: ObservanceState, previous: ObservanceState): Boolean {
        val tithiNumber = (state.tithiIndex % 15) + 1
        val festivalMonth = FestivalCatalog.festivalMonth(festival, state.monthReckoning)
        return (festivalMonth == null || festivalMonth == state.lunarMonth) &&
            (festival.paksha == null || festival.paksha == state.paksha) &&
            (festival.tithiNumber == null || festival.tithiNumber == tithiNumber) &&
            (festival.solarRashi == null || festival.solarRashi == state.solarRashiName) &&
            (festival.previousSolarRashi == null || festival.previousSolarRashi == previous.solarRashiName) &&
            (festival.nakshatra == null || festival.nakshatra == state.nakshatraName)
    }
    fun specialMatches(day: SpecialDay, state: ObservanceState): Boolean =
        day.month == state.lunarMonth && day.paksha == state.paksha && day.tithiNumber == (state.tithiIndex % 15) + 1
    fun userMatches(event: UserEvent, state: ObservanceState): Boolean =
        event.criteriaCount() >= 2 &&
            (event.month == null || event.month == state.lunarMonth) &&
            (event.paksha == null || event.paksha == state.paksha) &&
            (event.tithiIndex == null || event.tithiIndex == state.tithiIndex) &&
            (event.nakshatra == null || event.nakshatra == state.nakshatraName) &&
            (event.rashi == null || event.rashi == state.solarRashiName || event.rashi == state.lunarRashiName)

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
        firstMatching(candidates) { state, previous -> festivalMatches(festival, state, previous) }?.let {
            result += DayBrowserItem("festival:${festival.name}", festival.name, DaySection.Festivals, listOf(it))
        }
    }
    specialDays.forEach { day ->
        val key = "${day.name}:${day.month}:${day.paksha}:${day.tithiNumber}"
        val candidates = tithiIndexes(day.tithiNumber, day.paksha).flatMap(::tithiOccurrences)
        firstMatching(candidates) { state, _ -> specialMatches(day, state) }?.let {
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
        firstMatching(candidates) { state, _ -> userMatches(event, state) }?.let {
            result += DayBrowserItem("user:${event.identityKey()}", event.name, DaySection.User, listOf(it))
        }
    }
    return result.sortedWith(
        compareBy<DayBrowserItem> { item -> item.occurrences.firstOrNull()?.at }
            .thenBy { item -> item.name },
    )
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

private fun dayCacheKey(
    observer: Observer,
    now: ZonedDateTime,
    calendarConfigKey: String,
    specialDays: List<SpecialDay>,
    userEvents: List<UserEvent>,
): String = listOf(
    DAY_CACHE_VERSION,
    "%.4f".format(observer.latitude),
    "%.4f".format(observer.longitude),
    now.zone.id,
    now.toLocalDate().toString(),
    calendarConfigKey,
    specialDays.joinToString { "${it.name}:${it.month}:${it.paksha}:${it.tithiNumber}" },
    userEvents.joinToString { it.identityKey() },
).joinToString("|").hashCode().toString()

private fun loadCachedDayItems(context: Context, key: String): List<DayBrowserItem>? {
    val raw = context.getSharedPreferences(DAY_CACHE_PREFS, Context.MODE_PRIVATE).getString(key, null) ?: return null
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val value = array.getJSONObject(index)
            val occurrences = value.getJSONArray("occurrences")
            DayBrowserItem(
                key = value.getString("key"),
                name = value.getString("name"),
                defaultSection = DaySection.valueOf(value.getString("section")),
                recurring = value.getBoolean("recurring"),
                occurrences = List(occurrences.length()) { occurrence -> DayOccurrence(ZonedDateTime.parse(occurrences.getString(occurrence))) },
            )
        }
    }.getOrNull()
}

private fun saveCachedDayItems(context: Context, key: String, items: List<DayBrowserItem>) {
    val array = JSONArray()
    items.forEach { item ->
        array.put(JSONObject().apply {
            put("key", item.key)
            put("name", item.name)
            put("section", item.defaultSection.name)
            put("recurring", item.recurring)
            put("occurrences", JSONArray().apply { item.occurrences.forEach { put(it.at.toString()) } })
        })
    }
    context.getSharedPreferences(DAY_CACHE_PREFS, Context.MODE_PRIVATE).edit().clear().putString(key, array.toString()).apply()
}
