package dev.yantra.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.yantra.app.calendar.*
import dev.yantra.app.engine.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val DetailGold = Color(0xFFE8C477)
private val DetailIvory = Color(0xFFE7E5DD)
private val DetailMuted = Color(0xFF9BA2A8)

private data class DetailChartData(
    val catalog: StarCatalog,
    val snapshot: CelestialSnapshot,
    val frame: ChartFrame,
    val at: ZonedDateTime,
    val interval: Pair<ZonedDateTime, ZonedDateTime>,
    val otherIntervals: List<Pair<String, Pair<ZonedDateTime, ZonedDateTime>>>,
    val months: List<LunarMonthInterval> = emptyList(),
    val month: LunarMonthInterval? = null,
)

/** One detail shell and one chart pipeline, covering every catalogue entry. */
@Composable
internal fun AstronomicalDetailScreen(
    annotation: YantraAnnotation,
    reference: ZonedDateTime,
    calendarEngine: YantraCalendarEngine,
    observer: Observer,
    monthNames: MonthNameSet,
    reckoning: MonthReckoning,
    ayanamsa: Ayanamsa,
    onDismiss: () -> Unit,
    onShowDate: (ZonedDateTime) -> Unit,
) {
    val context = LocalContext.current
    val engine = remember(calendarEngine) { calendarEngine.fork() }
    val kind = annotation.kind
    var index by remember(annotation) { mutableStateOf(annotation.index) }
    var requestedTime by remember(annotation, reference) { mutableStateOf(reference) }
    var data by remember { mutableStateOf<DetailChartData?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var timelineOpen by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var retry by remember { mutableStateOf(0) }
    BackHandler(onBack = onDismiss)

    LaunchedEffect(kind, index, requestedTime, engine, retry) {
        loading = true
        failure = null
        val result = withContext(Dispatchers.Default) {
            try {
                val catalog = StarCatalog.load(context)
                val months = if (kind == AnnotationKind.Masa) engine.lunarMonthIntervals(requestedTime, observer) else emptyList()
                val month = if (kind == AnnotationKind.Masa) {
                    months.firstOrNull { it.monthIndex == index && it.contains(requestedTime) }
                        ?: months.firstOrNull { it.monthIndex == index && it.start.isAfter(requestedTime) }
                        ?: months.lastOrNull { it.monthIndex == index }
                        ?: error("This month name has no lunation in the surrounding calendar cycle.")
                } else null
                val at = if (month != null && !month.contains(requestedTime)) {
                    month.start.plusSeconds(Duration.between(month.start, month.end).seconds / 2)
                } else requestedTime
                val snapshot = engine.chartSnapshot(at)
                    ?: error("The offline ephemeris could not calculate this chart.")
                val interval = when (kind) {
                    AnnotationKind.Rashi -> engine.rashiInterval(at, observer, index, solar = true)
                    AnnotationKind.Nakshatra -> engine.nakshatraInterval(at, observer, index)
                    AnnotationKind.Masa -> month!!.start to month.end
                    else -> null
                } ?: error("The interval could not be calculated for this date.")
                val otherIntervals = if (kind == AnnotationKind.Rashi) {
                    listOfNotNull(engine.rashiInterval(at, observer, index, solar = false)?.let { "Chandra" to it })
                } else emptyList()
                val frame = if (kind == AnnotationKind.Masa) ChartFrame(snapshot.moon, 0.57)
                    else catalog.frame(catalog.pattern(if (kind == AnnotationKind.Rashi) "rashi" else "nakshatra", index))
                Result.success(DetailChartData(catalog, snapshot, frame, at, interval, otherIntervals, months, month))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
        data = result.getOrNull()
        failure = result.exceptionOrNull()?.message
        loading = false
    }

    fun changeSelection(direction: Int) {
        if (kind == AnnotationKind.Masa) {
            val current = data ?: return
            val position = current.months.indexOf(current.month)
            val next = current.months.getOrNull(position + direction) ?: return
            index = next.monthIndex
            requestedTime = if (next.contains(reference)) reference else next.start.plusSeconds(Duration.between(next.start, next.end).seconds / 2)
        } else {
            index = Math.floorMod(index + direction, if (kind == AnnotationKind.Rashi) 12 else 27)
        }
        timelineOpen = false
    }

    val title = when (kind) {
        AnnotationKind.Rashi -> CalendarCatalog.rashis[index].name
        AnnotationKind.Nakshatra -> CalendarCatalog.nakshatras[index].name
        else -> monthNames.monthNames[index]
    }
    val type = when (kind) {
        AnnotationKind.Rashi -> "RĀŚI"
        AnnotationKind.Nakshatra -> "NAKṢATRA"
        else -> "MĀSA"
    }
    val timePattern = if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val dateFormat = remember(timePattern) { DateTimeFormatter.ofPattern("d MMM yyyy · $timePattern") }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = DetailGold)

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        val chartHeight = (maxHeight * 0.50f).coerceIn(240.dp, 580.dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss, colors = buttonColors) { Text("‹ Back") }
                Text(type, color = DetailMuted, fontSize = 13.sp, letterSpacing = 2.sp)
                TextButton(onClick = { requestedTime = reference; index = annotation.index; timelineOpen = false }, colors = buttonColors) { Text("Reset") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { changeSelection(-1) }, enabled = !loading && data != null, colors = buttonColors) { Text("‹", fontSize = 28.sp) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, color = DetailGold, fontSize = 29.sp, fontFamily = FontFamily.Serif)
                    Text(when (kind) {
                        AnnotationKind.Rashi -> "${index + 1} of 12 · 30° sidereal interval"
                        AnnotationKind.Nakshatra -> "${index + 1} of 27 · 13°20′ sidereal interval"
                        else -> "${reckoning.displayName} · ${if (data?.month?.intercalary == true) "Adhika lunar month" else "Lunar month"}"
                    }, color = DetailMuted, fontSize = 12.sp)
                }
                TextButton(onClick = { changeSelection(1) }, enabled = !loading && data != null, colors = buttonColors) { Text("›", fontSize = 28.sp) }
            }
            if (loading) {
                Box(Modifier.fillMaxWidth().height(chartHeight), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DetailGold) }
            } else if (failure != null) {
                Column(Modifier.padding(vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(failure!!, color = DetailIvory)
                    TextButton(onClick = { retry++ }, colors = buttonColors) { Text("Try again") }
                }
            } else data?.let { chart ->
                CelestialChart(chart.catalog, chart.snapshot, chart.frame, kind, index, Modifier.fillMaxWidth().height(chartHeight))
                Text(chart.at.format(dateFormat) + " · " + chart.at.zone.id, color = DetailMuted, fontSize = 12.sp)
                Text("${ayanamsa.displayName} · ${"%.0f".format(chart.snapshot.illumination * 100)}% illuminated", color = DetailMuted, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier.fillMaxWidth().border(1.dp, DetailGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(when (kind) {
                        AnnotationKind.Rashi -> "Surya in $title"
                        AnnotationKind.Nakshatra -> "Chandra in $title"
                        else -> if (reckoning == MonthReckoning.Amanta) "New Moon to New Moon" else "Full Moon to Full Moon"
                    }, color = DetailGold, fontFamily = FontFamily.Serif, fontSize = 19.sp)
                    IntervalRows(chart.interval, dateFormat)
                    chart.otherIntervals.forEach { (label, interval) ->
                        HorizontalDivider(color = DetailGold.copy(alpha = 0.18f))
                        Text("$label in $title", color = DetailGold)
                        IntervalRows(interval, dateFormat)
                    }
                    chart.month?.let { month ->
                        val progress = month.progress(chart.at).toFloat()
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = DetailGold, trackColor = Color(0xFF25282B))
                        Text("${(progress * 100).toInt()}% through this lunar month", color = DetailMuted, fontSize = 12.sp)
                        month.skippedMonthIndex?.let { skipped ->
                            Text("Kshaya: ${monthNames.monthNames[skipped]} has no separate lunation in this cycle.", color = DetailMuted, fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = { timelineOpen = !timelineOpen }, colors = buttonColors, contentPadding = PaddingValues(0.dp)) {
                        Text(if (timelineOpen) "Close timeline ↑" else if (kind == AnnotationKind.Masa) "View month timeline →" else "View interval timeline →")
                    }
                    if (timelineOpen) {
                        DetailTimeline(chart, onTimeSelected = { requestedTime = it }, onShowDate = onShowDate, dateFormat = dateFormat)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (kind == AnnotationKind.Nakshatra) {
                    Text(listOf(Math.floorMod(index - 1, 27), index, (index + 1) % 27).joinToString(" → ") { CalendarCatalog.nakshatras[it].name }, color = DetailMuted, fontSize = 12.sp)
                }
                Text("Fixed celestial chart · north up", color = DetailMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun IntervalRows(interval: Pair<ZonedDateTime, ZonedDateTime>, format: DateTimeFormatter) {
    for ((label, time) in listOf("Starts" to interval.first, "Ends" to interval.second)) {
        Column {
            Text(label, color = DetailMuted, fontSize = 12.sp)
            Text(time.format(format), color = DetailIvory, fontSize = 15.sp)
        }
    }
}

@Composable
private fun DetailTimeline(
    data: DetailChartData,
    onTimeSelected: (ZonedDateTime) -> Unit,
    onShowDate: (ZonedDateTime) -> Unit,
    dateFormat: DateTimeFormatter,
) {
    val duration = Duration.between(data.interval.first, data.interval.second).seconds
    var fraction by remember(data) {
        mutableStateOf((Duration.between(data.interval.first, data.at).seconds.toFloat() / duration).coerceIn(0f, 1f))
    }
    // Stay within the half-open interval at the right endpoint.
    fun selectedTime() = data.interval.first.plusSeconds((duration * fraction).toLong().coerceIn(0, duration - 1))
    Text(selectedTime().format(dateFormat), color = DetailIvory, fontSize = 13.sp)
    Slider(
        value = fraction, onValueChange = { fraction = it },
        onValueChangeFinished = { onTimeSelected(selectedTime()) },
        colors = SliderDefaults.colors(thumbColor = DetailGold, activeTrackColor = DetailGold, inactiveTrackColor = Color(0xFF303338)),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Start", color = DetailMuted, fontSize = 12.sp)
        Text("End", color = DetailMuted, fontSize = 12.sp)
    }
    TextButton(onClick = { onShowDate(selectedTime()) }, colors = ButtonDefaults.textButtonColors(contentColor = DetailGold)) { Text("Show this time on Yantra") }
}
