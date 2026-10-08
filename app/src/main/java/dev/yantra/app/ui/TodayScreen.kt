package dev.yantra.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.yantra.app.calendar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val todayGold = Color(0xFFE8CA8B)
private val todayIvory = Color(0xFFFFE8B0)

@Composable
internal fun TodayOrb(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClickLabel = "Open today's details", onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(
            Brush.radialGradient(
                listOf(Color(0xFFFFF2BD), Color(0xFFE8BD58), Color(0xFF956019), Color(0xFF49300D)),
                center = Offset(24f, 20f), radius = 100f,
            )
        ).border(1.dp, todayGold, CircleShape))
        Text("Today", color = todayGold, style = MaterialTheme.typography.labelMedium)
    }
}

private data class TodayDetails(
    val at: ZonedDateTime,
    val state: YantraState,
    val observances: List<String>,
    val tithi: Pair<ZonedDateTime, ZonedDateTime>?,
    val nakshatra: Pair<ZonedDateTime, ZonedDateTime>?,
)

@Composable
internal fun TodayScreen(
    engine: YantraCalendarEngine,
    location: ObserverLocation,
    monthNames: MonthNameSet,
    ayanamsa: Ayanamsa,
    specialDays: List<SpecialDay>,
    userEvents: List<UserEvent>,
    onDismiss: () -> Unit,
) {
    var details by remember(engine, location) { mutableStateOf<TodayDetails?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(engine, location, specialDays, userEvents) {
        val calendar = engine.fork()
        while (true) {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    // Today always uses the observer's real date, including when the instrument previews another date.
                    val at = ZonedDateTime.now(location.zoneId)
                    val state = calendar.compute(at, location.observer)
                    val labels = linkedSetOf<String>()
                    val end = at.toLocalDate().plusDays(1).atStartOfDay(location.zoneId)
                    var sample = at.toLocalDate().atStartOfDay(location.zoneId)
                    while (sample.isBefore(end)) {
                        val sampled = calendar.compute(sample, location.observer)
                        specialDays.filter { it.matches(sampled) }.forEach { labels += it.name }
                        userEvents.filter { it.matches(sampled) }.forEach { labels += it.name }
                        FestivalCatalog.match(sampled, calendar.compute(sample.minusDays(1), location.observer))
                            ?.let { labels += it.name }
                        recurringObservanceName(sampled)?.let { labels += it }
                        sample = sample.plusMinutes(30)
                    }
                    TodayDetails(at, state, labels.toList(),
                        calendar.tithiInterval(at, location.observer, state.tithi.index),
                        calendar.nakshatraInterval(at, location.observer, state.nakshatra.index))
                }
            }
            failed = result.isFailure
            details = result.getOrNull()
            delay(60_000)
        }
    }
    Surface(Modifier.fillMaxSize(), color = Color(0xFF100C08)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Today", color = todayGold, style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onDismiss) { Text("Back", color = todayGold) }
            }
            Text(location.label + " · " + location.zoneId.id, color = todayGold)
            val data = details
            if (data == null) {
                if (failed) Text("Today's details could not be calculated. Retrying shortly.", color = todayIvory)
                else CircularProgressIndicator(color = todayGold)
            } else {
                Text(data.at.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                    color = todayIvory, style = MaterialTheme.typography.titleLarge)
                Text("Current at " + data.at.format(DateTimeFormatter.ofPattern("HH:mm z")), color = todayGold)
                TodaySection("Observances") {
                    Text(data.observances.joinToString("\n").ifEmpty { "No special observances today" }, color = todayIvory)
                }
                TodaySection("Calendar") {
                    TodayValue("Samvatsara", data.state.samvatsara.name)
                    TodayValue("Lunar month", monthNames.monthNames[data.state.month.index])
                    TodayValue("Reckoning", data.state.monthReckoning.displayName)
                    TodayValue("Paksha", data.state.paksha)
                    TodayValue("Tithi", data.state.tithi.name)
                    TodayInterval(data.tithi)
                    TodayValue("Nakshatra", data.state.nakshatra.name)
                    TodayInterval(data.nakshatra)
                    TodayValue("Yoga", data.state.yoga.name)
                }
                TodaySection("Sky now") {
                    TodayValue("Solar rashi", data.state.solarRashi.name)
                    TodayValue("Lunar rashi", data.state.lunarRashi.name)
                    TodayValue("Lagna", data.state.lagnaRashi?.name ?: "Unavailable")
                    TodayValue("Moon illumination", "${(data.state.moonIllumination * 100).roundToInt()}%")
                    TodayValue("Ayanamsa", ayanamsa.displayName)
                }
            }
        }
    }
}

@Composable
private fun TodaySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF21180E), MaterialTheme.shapes.medium).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = todayGold, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun TodayValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), color = todayGold)
        Text(value, Modifier.weight(1f), color = todayIvory)
    }
}

@Composable
private fun TodayInterval(interval: Pair<ZonedDateTime, ZonedDateTime>?) {
    val format = DateTimeFormatter.ofPattern("d MMM, HH:mm z")
    Text(interval?.let { "${it.first.format(format)} – ${it.second.format(format)}" }
        ?: "Timing unavailable", color = todayGold, style = MaterialTheme.typography.bodySmall)
}
