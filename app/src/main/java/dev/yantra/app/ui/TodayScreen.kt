package dev.yantra.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import dev.yantra.app.calendar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration

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
    sigilImages: SigilImages,
    specialDays: List<SpecialDay>,
    userEvents: List<UserEvent>,
    onDismiss: () -> Unit,
    onShowDate: (ZonedDateTime) -> Unit,
) {
    var details by remember(engine, location) { mutableStateOf<TodayDetails?>(null) }
    var selectedRashi by remember { mutableStateOf<Pair<RashiFocus, TodayDetails>?>(null) }
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
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF292014), Color(0xFF100C08), Color(0xFF17100A))))
                .safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val data = details
            if (data == null) {
                if (failed) Text("Today's details could not be calculated. Retrying shortly.", color = todayIvory)
                else CircularProgressIndicator(color = todayGold)
            } else {
                TodaySection {
                    Text(data.at.format(DateTimeFormatter.ofPattern("EEEE")), color = todayGold,
                        letterSpacing = 2.sp, style = MaterialTheme.typography.labelLarge)
                    Text(data.at.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                        color = todayIvory, fontFamily = FontFamily.Serif,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize * 0.8f,
                            lineHeight = MaterialTheme.typography.headlineMedium.lineHeight * 0.8f,
                        ))
                    HorizontalDivider(color = todayGold.copy(alpha = 0.25f))
                    Text("${data.state.samvatsara.name} samvatsara", color = todayGold,
                        fontFamily = FontFamily.Serif, style = MaterialTheme.typography.titleLarge)
                    Text("${monthNames.monthNames[data.state.month.index]} māsa · ${data.state.paksha} paksha",
                        color = todayIvory, style = MaterialTheme.typography.bodyLarge)
                    Text(tithiName(data.state.tithi.index), color = todayIvory,
                        fontFamily = FontFamily.Serif, style = MaterialTheme.typography.titleLarge)
                    TodayInterval(data.tithi)
                }
                TodaySection("Observances") {
                    if (data.observances.isEmpty()) Text("No special observances today", color = todayIvory.copy(alpha = 0.7f))
                    data.observances.forEach { label ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("✦", color = todayGold)
                            Text(label, color = todayIvory, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                TodaySection("Rāśi", inset = 14.dp, spacing = 8.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RashiTile("Solar", data.state.solarRashi, Modifier.weight(1f)) {
                            selectedRashi = RashiFocus.Solar to data
                        }
                        RashiTile("Lunar", data.state.lunarRashi, Modifier.weight(1f)) {
                            selectedRashi = RashiFocus.Lunar to data
                        }
                        RashiTile("Lagna", data.state.lagnaRashi, Modifier.weight(1f)) {
                            selectedRashi = RashiFocus.Lagna to data
                        }
                    }
                    Text("Tap a sign for its chart and duration", color = todayGold.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall)
                }
                TodaySection {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Canvas(Modifier.size(52.dp).background(Color(0xFF302314), CircleShape)) {
                            drawNakshatraSigil(data.state.nakshatra.index, center, size.minDimension * 0.68f, todayGold, sigilImages)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Nakṣatra", color = todayGold, style = MaterialTheme.typography.labelLarge)
                            Text(data.state.nakshatra.name, color = todayIvory,
                                fontFamily = FontFamily.Serif, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    TodayInterval(data.nakshatra)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(details?.let { "${location.label} · ${it.at.format(DateTimeFormatter.ofPattern("HH:mm z"))}" }
                    ?: location.label, Modifier.weight(1f),
                    color = todayGold.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDismiss) { Text("‹ Back", color = todayGold) }
            }
        }
    }
    selectedRashi?.let { (focus, selected) ->
        val sign = when (focus) {
            RashiFocus.Solar -> selected.state.solarRashi
            RashiFocus.Lunar -> selected.state.lunarRashi
            RashiFocus.Lagna -> selected.state.lagnaRashi
        }
        if (sign != null) AstronomicalDetailScreen(
            annotation = YantraAnnotation(AnnotationKind.Rashi, sign.index, sign.name),
            reference = selected.at,
            calendarEngine = engine,
            observer = location.observer,
            monthNames = monthNames,
            reckoning = selected.state.monthReckoning,
            ayanamsa = ayanamsa,
            rashiFocus = focus,
            onDismiss = { selectedRashi = null },
            onShowDate = onShowDate,
        )
    }
}

@Composable
private fun TodaySection(
    title: String? = null,
    inset: Dp = 12.dp,
    spacing: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF352719), Color(0xFF1C140D))), MaterialTheme.shapes.large)
            .border(1.dp, todayGold.copy(alpha = 0.24f), MaterialTheme.shapes.large).padding(inset),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        title?.let { Text(it, color = todayGold, fontFamily = FontFamily.Serif,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 24.sp)) }
        content()
    }
}

@Composable
private fun RashiTile(label: String, sign: Segment?, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium)
            .background(Color(0xFF100C08).copy(alpha = 0.45f))
            .clickable(enabled = sign != null, role = Role.Button,
                onClickLabel = "View $label rashi chart and duration", onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = todayGold, style = MaterialTheme.typography.labelLarge)
        Text(sign?.let { zodiacSymbols[it.index] } ?: "—", color = todayGold,
            fontSize = 30.4.sp, lineHeight = 36.8.sp, textAlign = TextAlign.Center)
        Text(sign?.name ?: "Unavailable", color = todayIvory, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium)
    }
}

private val zodiacSymbols = listOf("♈︎", "♉︎", "♊︎", "♋︎", "♌︎", "♍︎", "♎︎", "♏︎", "♐︎", "♑︎", "♒︎", "♓︎")
private val tithiNames = listOf("Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami", "Shashthi",
    "Saptami", "Ashtami", "Navami", "Dashami", "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi")
private fun tithiName(index: Int): String = when (index) {
    14 -> "Purnima"
    29 -> "Amavasya"
    else -> tithiNames[index % 15]
}

@Composable
private fun TodayInterval(interval: Pair<ZonedDateTime, ZonedDateTime>?) {
    val format = DateTimeFormatter.ofPattern("d MMM, HH:mm z")
    if (interval == null) {
        Text("Timing unavailable", color = todayGold, style = MaterialTheme.typography.bodySmall)
    } else {
        val minutes = Duration.between(interval.first, interval.second).toMinutes()
        Text("${interval.first.format(format)} – ${interval.second.format(format)}",
            color = todayGold, style = MaterialTheme.typography.bodySmall)
        Text("Duration · ${minutes / 60}h ${minutes % 60}m", color = todayIvory.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall)
    }
}
