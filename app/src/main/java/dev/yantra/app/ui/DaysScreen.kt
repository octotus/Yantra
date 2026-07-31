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
    var editMode by remember { mutableStateOf(false) }
    val openSections = remember { mutableStateMapOf<DaySection, Boolean>() }
    val openRecurring = remember { mutableStateMapOf<String, Boolean>() }
    val overrides = remember { mutableStateMapOf<String, DaySection>().apply { putAll(loadDaySections(context)) } }

    LaunchedEffect(engine, observer, now.toLocalDate(), specialDays, userEvents) {
        loading = true
        items = withContext(Dispatchers.Default) {
            buildDayItems(engine, observer, now, specialDays, userEvents)
        }
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
    recurringTithis.forEach { recurring ->
        val occurrences = recurring.indexes.flatMap { index ->
            val found = mutableListOf<DayOccurrence>()
            var cursor = now
            while (cursor.isBefore(end)) {
                val interval = engine.tithiInterval(cursor, observer, index) ?: break
                if (interval.first.isAfter(end)) break
                if (!interval.first.isBefore(now)) found += DayOccurrence(interval.first)
                cursor = interval.second.plusMinutes(1)
            }
            found
        }.distinctBy { it.at.toLocalDate() }.sortedBy { it.at }
        result += DayBrowserItem("recurring:${recurring.name}", recurring.name, DaySection.Special, occurrences, recurring = true)
    }
    val annualFestivals = FestivalCatalog.definitions.filterNot { it.month == null && it.solarRashi == null && it.tithiNumber in listOf(11, 13) }
    val festivalDates = mutableMapOf<String, ZonedDateTime>()
    val specialDates = mutableMapOf<String, ZonedDateTime>()
    val userDates = mutableMapOf<String, ZonedDateTime>()
    var candidate = now.toLocalDate().atTime(12, 0).atZone(now.zone)
    while (!candidate.isAfter(end) && (festivalDates.size < annualFestivals.size || specialDates.size < specialDays.size || userDates.size < userEvents.size)) {
        val state = engine.compute(candidate, observer)
        val previous = engine.compute(candidate.minusDays(1), observer)
        annualFestivals.forEach { festival ->
            if (festival.name !in festivalDates && FestivalCatalog.matches(festival, state, previous)) festivalDates[festival.name] = candidate
        }
        specialDays.forEach { day ->
            val key = "${day.name}:${day.month}:${day.paksha}:${day.tithiNumber}"
            if (key !in specialDates && day.matches(state)) specialDates[key] = candidate
        }
        userEvents.forEach { event ->
            val key = event.identityKey()
            if (key !in userDates && event.matches(state)) userDates[key] = candidate
        }
        candidate = candidate.plusDays(1)
    }
    annualFestivals.forEach { festival ->
        festivalDates[festival.name]?.let { result += DayBrowserItem("festival:${festival.name}", festival.name, DaySection.Festivals, listOf(DayOccurrence(it))) }
    }
    specialDays.forEach { day ->
        val key = "${day.name}:${day.month}:${day.paksha}:${day.tithiNumber}"
        specialDates[key]?.let { result += DayBrowserItem("special:$key", day.name, DaySection.Special, listOf(DayOccurrence(it))) }
    }
    userEvents.forEach { event ->
        userDates[event.identityKey()]?.let { result += DayBrowserItem("user:${event.identityKey()}", event.name, DaySection.User, listOf(DayOccurrence(it))) }
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
