package dev.yantra.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.yantra.app.calendar.CalendarCatalog
import dev.yantra.app.calendar.FinderCriteria
import dev.yantra.app.calendar.FinderResult
import dev.yantra.app.calendar.MonthNameSet
import dev.yantra.app.calendar.YantraCalendarEngine
import dev.yantra.app.calendar.YantraState
import dev.yantra.app.engine.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private enum class FinderRing { Samvatsara, Tithi, Nakshatra, Month, Rashi }

@Composable
internal fun YantraFinderScreen(
    engine: YantraCalendarEngine,
    observer: Observer,
    reference: ZonedDateTime,
    initialState: YantraState,
    monthNameSet: MonthNameSet,
    onDismiss: () -> Unit,
    onLoadResult: (ZonedDateTime) -> Unit,
) {
    var samvatsaraOffset by remember { mutableStateOf(0) }
    var tithiIndex by remember { mutableStateOf(initialState.tithi.index) }
    var nakshatraIndex by remember { mutableStateOf(initialState.nakshatra.index) }
    var monthIndex by remember { mutableStateOf(initialState.month.index) }
    var rashiIndex by remember { mutableStateOf(initialState.solarRashi.index) }
    var tithiActive by remember { mutableStateOf(false) }
    var nakshatraActive by remember { mutableStateOf(false) }
    var monthActive by remember { mutableStateOf(false) }
    var rashiActive by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<FinderResult?>(null) }
    var noResult by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedSamvatsara = CalendarCatalog.samvatsaras[Math.floorMod(initialState.samvatsara.index + samvatsaraOffset, 60)]

    fun solve() {
        val criteria = FinderCriteria(
            monthIndex = monthIndex.takeIf { monthActive },
            tithiIndex = tithiIndex.takeIf { tithiActive },
            nakshatraIndex = nakshatraIndex.takeIf { nakshatraActive },
            solarRashiIndex = rashiIndex.takeIf { rashiActive },
        )
        if (criteria.isEmpty || searching) return
        searching = true
        noResult = false
        scope.launch {
            result = withContext(Dispatchers.Default) {
                engine.findNextOccurrence(reference, observer, samvatsaraOffset, criteria)
            }
            noResult = result == null
            searching = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(0f to Color(0xFF3A1D0C), 0.58f to Color(0xFF100905), 1f to Color(0xFF030201))
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFE2A3))) { Text("Back") }
                Text("ALIGN AT 12 · PRESS CENTRE", color = Color(0xFFE8CA8B), modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    tithiActive = false; nakshatraActive = false; monthActive = false; rashiActive = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFE2A3))) { Text("Clear") }
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    var activeRing: FinderRing? = null
                    var accumulated = 0f
                    detectDragGestures(
                        onDragStart = { point ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = min(size.width, size.height) * 0.43f
                            val distance = hypot(point.x - center.x, point.y - center.y)
                            activeRing = when {
                                distance >= radius * 0.88f -> FinderRing.Samvatsara
                                distance >= radius * 0.68f -> FinderRing.Tithi
                                distance >= radius * 0.53f -> FinderRing.Nakshatra
                                distance >= radius * 0.38f -> FinderRing.Month
                                distance >= radius * 0.23f -> FinderRing.Rashi
                                else -> null
                            }
                            accumulated = 0f
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            accumulated += drag.x - drag.y
                            if (kotlin.math.abs(accumulated) >= 22f) {
                                val step = if (accumulated > 0f) 1 else -1
                                when (activeRing) {
                                    FinderRing.Samvatsara -> samvatsaraOffset += step
                                    FinderRing.Tithi -> { tithiIndex = Math.floorMod(tithiIndex + step, 30); tithiActive = true }
                                    FinderRing.Nakshatra -> { nakshatraIndex = Math.floorMod(nakshatraIndex + step, 27); nakshatraActive = true }
                                    FinderRing.Month -> { monthIndex = Math.floorMod(monthIndex + step, 12); monthActive = true }
                                    FinderRing.Rashi -> { rashiIndex = Math.floorMod(rashiIndex + step, 12); rashiActive = true }
                                    null -> Unit
                                }
                                accumulated = 0f
                            }
                        },
                        onDragEnd = { activeRing = null },
                        onDragCancel = { activeRing = null },
                    )
                }
                .pointerInput(searching, tithiActive, nakshatraActive, monthActive, rashiActive, samvatsaraOffset) {
                    detectTapGestures { point ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) * 0.43f
                        if (hypot(point.x - center.x, point.y - center.y) <= radius * 0.18f) solve()
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.43f
            val gold = Color(0xFFFFD992)
            val bronze = Color(0xFF7A5734)
            drawCircle(Color(0xFF0B0906), radius, center)
            drawCircle(gold.copy(alpha = 0.8f), radius, center, style = Stroke(radius * 0.012f))
            drawFinderRing(center, radius * 0.76f, 30, tithiIndex, tithiActive, (1..15).map(Int::toString) + (1..15).map(Int::toString), gold, bronze)
            drawFinderRing(center, radius * 0.60f, 27, nakshatraIndex, nakshatraActive, CalendarCatalog.nakshatras.map { it.name.take(3).uppercase() }, gold, bronze)
            drawFinderRing(center, radius * 0.45f, 12, monthIndex, monthActive, monthNameSet.abbreviations, gold, bronze)
            drawFinderRing(center, radius * 0.30f, 12, rashiIndex, rashiActive, CalendarCatalog.rashis.map { it.name.take(3).uppercase() }, gold, bronze)
            drawCircle(if (searching) bronze else gold.copy(alpha = 0.18f), radius * 0.17f, center)
            drawCircle(gold, radius * 0.17f, center, style = Stroke(radius * 0.009f))
            drawFinderText(center, if (searching) "SEARCHING" else "FIND", radius * 0.055f, gold)
            drawFinderText(Offset(center.x, center.y - radius * 0.92f), selectedSamvatsara.name.uppercase(), radius * 0.052f, gold)
            drawLine(gold, Offset(center.x, center.y - radius), Offset(center.x, center.y - radius * 0.82f), radius * 0.012f)
        }

        if (searching) CircularProgressIndicator(color = Color(0xFFFFD992), modifier = Modifier.align(Alignment.BottomCenter).padding(28.dp))
        if (noResult) {
            AlertDialog(
                onDismissRequest = { noResult = false },
                title = { Text("No occurrence") },
                text = { Text("The selected alignment does not occur during ${selectedSamvatsara.name}.") },
                confirmButton = { TextButton(onClick = { noResult = false }) { Text("Close") } },
            )
        }
        result?.let { found ->
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy · h:mm a z")
            AlertDialog(
                onDismissRequest = { result = null },
                title = { Text("${found.state.samvatsara.name} alignment") },
                text = { Text(found.dateTime.format(formatter)) },
                confirmButton = { TextButton(onClick = { onLoadResult(found.dateTime) }) { Text("Load in Yantra") } },
                dismissButton = { TextButton(onClick = { result = null }) { Text("Keep searching") } },
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderRing(
    center: Offset,
    radius: Float,
    count: Int,
    selected: Int,
    enabled: Boolean,
    labels: List<String>,
    gold: Color,
    bronze: Color,
) {
    val color = if (enabled) gold else bronze
    drawCircle(color.copy(alpha = 0.8f), radius, center, style = Stroke(radius * 0.035f))
    val textSize = radius * if (count > 20) 0.075f else 0.105f
    repeat(count) { index ->
        val angle = Math.toRadians(-90.0 + (index - selected) * 360.0 / count)
        val position = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
        drawFinderText(position, labels.getOrElse(index) { "" }, textSize, if (index == selected && enabled) gold else color.copy(alpha = 0.72f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderText(center: Offset, text: String, size: Float, color: Color) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = size
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        }
        canvas.nativeCanvas.drawText(text, center.x, center.y + size * 0.34f, paint)
    }
}
