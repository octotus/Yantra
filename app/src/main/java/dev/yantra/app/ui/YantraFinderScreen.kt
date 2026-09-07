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
import kotlin.math.hypot

private enum class FinderRing { Samvatsara, Tithi, Nakshatra, Month, Rashi }

@Composable
internal fun YantraFinderScreen(
    engine: YantraCalendarEngine,
    observer: Observer,
    reference: ZonedDateTime,
    initialState: YantraState,
    sigilImages: SigilImages,
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
    val displayState = initialState.copy(
        samvatsara = selectedSamvatsara,
        tithi = CalendarCatalog.tithis[tithiIndex],
        nakshatra = CalendarCatalog.nakshatras[nakshatraIndex],
        month = initialState.monthSectors[monthIndex],
        rashi = CalendarCatalog.rashis[rashiIndex],
        solarRashi = CalendarCatalog.rashis[rashiIndex],
    )

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
            Brush.radialGradient(
                0.0f to Color(0xFF100C08),
                0.55f to Color(0xFF070504),
                1.0f to Color(0xFF050403),
            )
        )
    ) {
        YantraInstrument(
            state = displayState,
            sigilImages = sigilImages,
            monthNameSet = monthNameSet,
            lunarEmphasis = false,
            festival = null,
            observanceLabel = null,
            showFestivalLabel = false,
            annotation = null,
            showBack = false,
            userLogo = null,
            userLogoLit = false,
            now = reference,
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 10.dp),
            onMoonTap = {},
            onMoonLongPress = {},
            onDateTap = {},
            onDateLongPress = {},
            onSettingsTap = {},
            onBackTap = {},
            onFestivalTap = {},
            onAnnotation = {},
            onAnnotationDismiss = {},
        )

        Canvas(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 10.dp)
                .pointerInput(Unit) {
                    var activeRing: FinderRing? = null
                    var accumulated = 0f
                    detectDragGestures(
                        onDragStart = { point ->
                            val layout = yantraLayout(size.width.toFloat(), size.height.toFloat())
                            val center = layout.center
                            val radius = layout.radius
                            val distance = hypot(point.x - center.x, point.y - center.y)
                            activeRing = when {
                                distance >= radius * 1.04f -> FinderRing.Samvatsara
                                distance >= radius * 0.875f -> FinderRing.Tithi
                                distance >= radius * 0.70f -> FinderRing.Nakshatra
                                distance >= radius * 0.57f -> FinderRing.Month
                                distance >= radius * 0.38f -> FinderRing.Rashi
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
                        val layout = yantraLayout(size.width.toFloat(), size.height.toFloat())
                        val center = layout.center
                        val radius = layout.radius
                        if (hypot(point.x - center.x, point.y - center.y) <= radius * 0.18f) solve()
                    }
                }
        ) {
        }

        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFE2A3))) { Text("Back") }
                Text("ALIGN AT 12 · PRESS CENTRE", color = Color(0xFFE8CA8B), modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    tithiActive = false; nakshatraActive = false; monthActive = false; rashiActive = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFE2A3))) { Text("Clear") }
            }
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
