package dev.yantra.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.yantra.app.R
import dev.yantra.app.calendar.CalendarCatalog
import dev.yantra.app.calendar.LagnaSector
import dev.yantra.app.calendar.MonthSector
import dev.yantra.app.calendar.MonthNameSet
import dev.yantra.app.calendar.MonthReckoning
import dev.yantra.app.calendar.YantraCalendarEngine
import dev.yantra.app.calendar.YantraState
import dev.yantra.app.engine.AstronomyEngine
import dev.yantra.app.engine.EphemerisAssets
import dev.yantra.app.engine.Observer
import dev.yantra.app.engine.SwissEphemeris
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun YantraApp() {
    val context = LocalContext.current
    val sigilImages = SigilImages(
        maghaCrown = ImageBitmap.imageResource(id = R.drawable.sigil_magha_crown),
        pushyaFlower = ImageBitmap.imageResource(id = R.drawable.sigil_pushya_flower),
        purvaPhalguniPavilion = ImageBitmap.imageResource(id = R.drawable.sigil_purva_phalguni_pavilion),
    )
    var monthNameSetId by remember(context) { mutableStateOf(loadMonthNameSetId(context)) }
    var calendarLocaleRuleId by remember(context) { mutableStateOf(loadCalendarLocaleRuleId(context)) }
    var monthReckoningId by remember(context) { mutableStateOf(loadMonthReckoningId(context)) }
    val monthNameSet = remember(monthNameSetId) { selectedMonthNameSet(monthNameSetId) }
    val calendarLocaleRule = remember(calendarLocaleRuleId) { selectedCalendarLocaleRule(calendarLocaleRuleId) }
    val monthReckoning = remember(monthReckoningId) { selectedMonthReckoning(monthReckoningId) }
    val engine = remember(context, calendarLocaleRuleId, monthReckoningId) {
        val ephemerisDirectory = EphemerisAssets(context).install()
        YantraCalendarEngine(
            AstronomyEngine(
                longitudeProvider = SwissEphemeris(ephemerisDirectory.absolutePath)
            ),
            calendarLocaleRule = calendarLocaleRule,
            monthReckoning = monthReckoning,
        )
    }
    var observerLocation by remember(context) { mutableStateOf(loadObserverLocation(context)) }
    val observer = remember(observerLocation) { observerLocation.observer }
    var now by remember(observerLocation.timeZoneId) { mutableStateOf(ZonedDateTime.now(observerLocation.zoneId)) }
    var datePreviewActive by remember { mutableStateOf(false) }
    val state = remember(now, engine, observer) { engine.compute(now, observer) }
    val previousState = remember(now, engine, observer) { engine.compute(now.minusDays(1), observer) }
    val festival = remember(state, previousState) { FestivalCatalog.match(state, previousState) }
    val todayFestival = remember(engine, observer, observerLocation.timeZoneId, now.toLocalDate()) {
        val today = ZonedDateTime.now(observerLocation.zoneId).toLocalDate().atTime(12, 0).atZone(observerLocation.zoneId)
        FestivalCatalog.match(engine.compute(today, observer), engine.compute(today.minusDays(1), observer))
    }
    var specialDays by remember(context) { mutableStateOf(loadSpecialDays(context)) }
    val specialDay = remember(state, specialDays) { specialDays.firstOrNull { it.matches(state) } }
    var userEvents by remember(context) { mutableStateOf(loadUserEvents(context)) }
    var logoPath by remember(context) { mutableStateOf(loadUserLogoPath(context)) }
    val userLogo = remember(logoPath) { logoPath?.let(::loadLogoBitmap) }
    val userEvent = remember(state, userEvents) { userEvents.firstOrNull { it.matches(state) } }
    val observanceLabel = specialDay?.name ?: userEvent?.name ?: festival?.name
    var lunarEmphasis by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var specialDayEditorOpen by remember { mutableStateOf(false) }
    var daysScreenOpen by remember { mutableStateOf(false) }
    var festivalLabelVisible by remember { mutableStateOf(false) }
    var annotation by remember { mutableStateOf<YantraAnnotation?>(null) }
    val annotationScope = rememberCoroutineScope()
    var notificationEnabled by remember(context) { mutableStateOf(notificationsEnabled(context)) }
    var notificationExplanationOpen by remember(context) {
        mutableStateOf(notificationEnabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            notificationEnabled = false
            setNotificationsEnabled(context, false)
        } else ObservanceNotificationScheduler.schedule(context)
    }

    LaunchedEffect(observerLocation, notificationEnabled) {
        ObservanceNotificationScheduler.createChannel(context)
        if (notificationEnabled && (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
            ObservanceNotificationScheduler.schedule(context)
        }
        while (true) {
            if (!datePreviewActive) {
                now = ZonedDateTime.now(observerLocation.zoneId)
            }
            delay(60_000)
        }
    }

    LaunchedEffect(lunarEmphasis) {
        if (lunarEmphasis) {
            delay(5_000)
            lunarEmphasis = false
        }
    }

    LaunchedEffect(festivalLabelVisible, observanceLabel) {
        if (festivalLabelVisible) {
            delay(3_000)
            festivalLabelVisible = false
        }
    }

    BackHandler(enabled = datePickerOpen || settingsOpen || daysScreenOpen || specialDayEditorOpen || annotation != null || datePreviewActive) {
        when {
            datePickerOpen -> datePickerOpen = false
            settingsOpen -> settingsOpen = false
            daysScreenOpen -> daysScreenOpen = false
            specialDayEditorOpen -> specialDayEditorOpen = false
            annotation != null -> annotation = null
            datePreviewActive -> {
                datePreviewActive = false
                now = ZonedDateTime.now(observerLocation.zoneId)
            }
        }
    }

    MaterialTheme {
        Surface(color = Color(0xFF0B0906), modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.0f to Color(0xFF100C08),
                            0.55f to Color(0xFF070504),
                            1.0f to Color(0xFF050403),
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    YantraInstrument(
                        state = state,
                        sigilImages = sigilImages,
                        monthNameSet = monthNameSet,
                        lunarEmphasis = lunarEmphasis,
                        festival = festival,
                        observanceLabel = observanceLabel,
                        showFestivalLabel = festivalLabelVisible,
                        annotation = annotation,
                        showBack = datePreviewActive,
                        userLogo = userLogo,
                        userLogoLit = userLogo != null && userEvent != null,
                        now = now,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize(),
                        onMoonTap = { lunarEmphasis = true },
                        onDateTap = { datePickerOpen = true },
                        onDateLongPress = { specialDayEditorOpen = true },
                        onSettingsTap = { settingsOpen = true },
                        onBackTap = {
                            datePreviewActive = false
                            now = ZonedDateTime.now(observerLocation.zoneId)
                        },
                        onFestivalTap = {
                            daysScreenOpen = true
                        },
                        onAnnotation = { tapped ->
                            annotation = tapped
                            annotationScope.launch {
                                val detailed = withContext(Dispatchers.Default) {
                                    tapped.withDuration(context, now, state, engine, observer)
                                }
                                if (annotation?.kind == tapped.kind && annotation?.index == tapped.index) {
                                    annotation = detailed
                                }
                            }
                        },
                        onAnnotationDismiss = { annotation = null },
                    )
                    if (daysScreenOpen) {
                        DaysScreen(
                            context = context,
                            engine = engine,
                            observer = observer,
                            now = ZonedDateTime.now(observerLocation.zoneId),
                            todayFestival = todayFestival,
                            calendarConfigKey = "$calendarLocaleRuleId|$monthReckoningId",
                            specialDays = specialDays,
                            userEvents = userEvents,
                            onDismiss = { daysScreenOpen = false },
                            onSelect = { occurrence ->
                                now = occurrence.withZoneSameInstant(observerLocation.zoneId)
                                datePreviewActive = true
                                daysScreenOpen = false
                            },
                            onAddDays = {
                                daysScreenOpen = false
                                settingsOpen = true
                            },
                        )
                    }
                    if (datePickerOpen) {
                        CryptexDatePicker(
                            selected = now,
                            modifier = Modifier.fillMaxSize(),
                            onDismiss = { datePickerOpen = false },
                            onDateSelected = { selected ->
                                now = selected
                                datePreviewActive = true
                                datePickerOpen = false
                            },
                        )
                    }
                    if (settingsOpen) {
                        YantraSettingsScreen(
                            logoPath = logoPath,
                            events = userEvents,
                            specialDays = specialDays,
                            monthNameSetId = monthNameSetId,
                            calendarLocaleRuleId = calendarLocaleRuleId,
                            monthReckoningId = monthReckoningId,
                            observerLocation = observerLocation,
                            notificationsEnabled = notificationEnabled,
                            onDismiss = { settingsOpen = false },
                            onLogoSelected = { uri ->
                                val path = saveUserLogo(context, uri)
                                saveUserLogoPath(context, path)
                                logoPath = path
                            },
                            onLogoCleared = {
                                clearUserLogo(context)
                                logoPath = null
                            },
                            onEventsChanged = { next ->
                                saveUserEvents(context, next)
                                userEvents = next
                            },
                            onSpecialDaysChanged = { next ->
                                saveSpecialDays(context, next)
                                specialDays = next
                            },
                            onMonthNameSetChanged = { id ->
                                saveMonthNameSetId(context, id)
                                monthNameSetId = id
                            },
                            onCalendarLocaleRuleChanged = { id ->
                                saveCalendarLocaleRuleId(context, id)
                                calendarLocaleRuleId = id
                            },
                            onMonthReckoningChanged = { id ->
                                saveMonthReckoningId(context, id)
                                monthReckoningId = id
                            },
                            onObserverLocationChanged = { next ->
                                observerLocation = next
                                now = now.withZoneSameInstant(next.zoneId)
                                datePreviewActive = false
                                ObservanceNotificationScheduler.reschedule(context)
                            },
                            onNotificationsChanged = { enabled ->
                                notificationEnabled = enabled
                                setNotificationsEnabled(context, enabled)
                                if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationExplanationOpen = true
                                }
                            },
                        )
                    }
                    if (notificationExplanationOpen) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { notificationExplanationOpen = false },
                            title = { androidx.compose.material3.Text("Quiet important-day reminders") },
                            text = { androidx.compose.material3.Text("Yantra places a quiet symbol in the status bar on important festival days, user days, and Amavasya. It makes no sound or vibration and expires when the event ends. Schedules stay on this device.") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    notificationExplanationOpen = false
                                    if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }) { androidx.compose.material3.Text("Allow notifications") }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    notificationExplanationOpen = false
                                    notificationEnabled = false
                                    setNotificationsEnabled(context, false)
                                }) { androidx.compose.material3.Text("Not now") }
                            },
                        )
                    }
                    if (specialDayEditorOpen) {
                        SpecialDayEditor(
                            state = state,
                            savedDays = specialDays.filter { it.matches(state) },
                            monthNameSet = monthNameSet,
                            onDismiss = { specialDayEditorOpen = false },
                            onSave = { name ->
                                val next = (specialDays + state.toSpecialDay(name))
                                    .distinctBy { listOf(it.name, it.month, it.paksha, it.tithiNumber).joinToString("|") }
                                saveSpecialDays(context, next)
                                specialDays = next
                                specialDayEditorOpen = false
                                festivalLabelVisible = true
                            },
                            onDelete = { day ->
                                val next = specialDays.filterNot { it == day }
                                saveSpecialDays(context, next)
                                specialDays = next
                                specialDayEditorOpen = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialDayEditor(
    state: YantraState,
    savedDays: List<SpecialDay>,
    monthNameSet: MonthNameSet,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: (SpecialDay) -> Unit,
) {
    var name by remember(state) { mutableStateOf("") }
    var selectedSavedIndex by remember(state, savedDays) { mutableStateOf(0) }
    val selectedSavedDay = savedDays.getOrNull(selectedSavedIndex.coerceAtMost((savedDays.size - 1).coerceAtLeast(0)))
    val tithiNumber = (state.tithi.index % 15) + 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Special day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${localizedMonthName(state.month.index, monthNameSet)} ${state.paksha} $tithiNumber")
                if (selectedSavedDay != null) {
                    Text("Saved: ${selectedSavedDay.name}")
                    if (savedDays.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selectedSavedIndex = (selectedSavedIndex - 1).floorMod(savedDays.size) }) {
                                Text("Previous")
                            }
                            TextButton(onClick = { selectedSavedIndex = (selectedSavedIndex + 1).floorMod(savedDays.size) }) {
                                Text("Next")
                            }
                        }
                    }
                    TextButton(onClick = { onDelete(selectedSavedDay) }) {
                        Text("Delete saved")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("New name") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotBlank()) onSave(trimmed)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CryptexDatePicker(
    selected: ZonedDateTime,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onDateSelected: (ZonedDateTime) -> Unit,
) {
    var year by remember(selected) { mutableStateOf(selected.year) }
    var month by remember(selected) { mutableStateOf(selected.monthValue) }
    var day by remember(selected) { mutableStateOf(selected.dayOfMonth) }
    val clampedDay = day.coerceIn(1, YearMonth.of(year, month).lengthOfMonth())

    Canvas(
        modifier = modifier.pointerInput(year, month, day) {
            detectTapGestures { tap ->
                val layout = cryptexLayout(size.width.toFloat(), size.height.toFloat())
                if (!tap.isInRect(layout.outerRect)) {
                    onDismiss()
                    return@detectTapGestures
                }
                val column = layout.columns.indexOfFirst { tap.isInRect(it) }
                if (column < 0) {
                    if (tap.isInRect(layout.commitRect)) {
                        onDateSelected(selected.withYear(year).withMonth(month).withDayOfMonth(clampedDay))
                    }
                    return@detectTapGestures
                }
                val zoneTop = layout.columns[column].top + layout.columns[column].height * 0.34f
                val zoneBottom = layout.columns[column].top + layout.columns[column].height * 0.66f
                when {
                    tap.y < zoneTop -> {
                        when (column) {
                            0 -> year -= 1
                            1 -> {
                                month = if (month == 1) 12 else month - 1
                                day = day.coerceAtMost(YearMonth.of(year, month).lengthOfMonth())
                            }
                            2 -> day = if (day == 1) YearMonth.of(year, month).lengthOfMonth() else day - 1
                        }
                    }
                    tap.y > zoneBottom -> {
                        when (column) {
                            0 -> year += 1
                            1 -> {
                                month = if (month == 12) 1 else month + 1
                                day = day.coerceAtMost(YearMonth.of(year, month).lengthOfMonth())
                            }
                            2 -> day = if (day == YearMonth.of(year, month).lengthOfMonth()) 1 else day + 1
                        }
                    }
                    else -> onDateSelected(selected.withYear(year).withMonth(month).withDayOfMonth(clampedDay))
                }
            }
        }
    ) {
        drawCryptexPicker(
            layout = cryptexLayout(size.width, size.height),
            year = year,
            month = month,
            day = clampedDay,
        )
    }
}

private data class CryptexLayout(
    val outerRect: Rect,
    val columns: List<Rect>,
    val commitRect: Rect,
)

internal enum class FestivalRank {
    Major,
    Minor,
}

internal data class FestivalDefinition(
    val name: String,
    val rank: FestivalRank,
    val month: String? = null,
    val paksha: String? = null,
    val tithiNumber: Int? = null,
    val solarRashi: String? = null,
    val previousSolarRashi: String? = null,
    val nakshatra: String? = null,
    val astronomicalDefinition: String,
)

internal data class SpecialDay(
    val name: String,
    val month: String,
    val paksha: String,
    val tithiNumber: Int,
) {
    fun matches(state: YantraState): Boolean =
        month == state.lunarMonth &&
            paksha == state.paksha &&
            tithiNumber == (state.tithi.index % 15) + 1
}

private const val SPECIAL_DAYS_PREFS = "yantra_special_days"
private const val SPECIAL_DAYS_KEY = "days"

private fun YantraState.toSpecialDay(name: String): SpecialDay =
    SpecialDay(
        name = name,
        month = lunarMonth,
        paksha = paksha,
        tithiNumber = (tithi.index % 15) + 1,
    )

internal fun loadSpecialDays(context: Context): List<SpecialDay> =
    context.getSharedPreferences(SPECIAL_DAYS_PREFS, Context.MODE_PRIVATE)
        .getStringSet(SPECIAL_DAYS_KEY, emptySet())
        .orEmpty()
        .mapNotNull { encoded ->
            val parts = encoded.split("|")
            if (parts.size != 4) return@mapNotNull null
            val tithiNumber = parts[3].toIntOrNull() ?: return@mapNotNull null
            SpecialDay(Uri.decode(parts[0]), parts[1], parts[2], tithiNumber)
        }
        .sortedWith(compareBy<SpecialDay> { it.month }.thenBy { it.paksha }.thenBy { it.tithiNumber }.thenBy { it.name })

private fun saveSpecialDays(context: Context, days: List<SpecialDay>) {
    val encoded = days.map { day ->
        listOf(Uri.encode(day.name), day.month, day.paksha, day.tithiNumber.toString()).joinToString("|")
    }.toSet()
    context.getSharedPreferences(SPECIAL_DAYS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putStringSet(SPECIAL_DAYS_KEY, encoded)
        .apply()
}

@Composable
private fun YantraSettingsScreen(
    logoPath: String?,
    events: List<UserEvent>,
    specialDays: List<SpecialDay>,
    monthNameSetId: String,
    calendarLocaleRuleId: String,
    monthReckoningId: String,
    observerLocation: ObserverLocation,
    notificationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onLogoSelected: (Uri) -> Unit,
    onLogoCleared: () -> Unit,
    onEventsChanged: (List<UserEvent>) -> Unit,
    onSpecialDaysChanged: (List<SpecialDay>) -> Unit,
    onMonthNameSetChanged: (String) -> Unit,
    onCalendarLocaleRuleChanged: (String) -> Unit,
    onMonthReckoningChanged: (String) -> Unit,
    onObserverLocationChanged: (ObserverLocation) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val logoBitmap = remember(logoPath) { logoPath?.let(::loadLogoBitmap) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onLogoSelected(uri)
    }
    var selectedIndex by remember(events) { mutableStateOf(-1) }
    var draft by remember(events, selectedIndex) {
        mutableStateOf((events.getOrNull(selectedIndex) ?: UserEvent()).toDraft())
    }
    var importText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var savedDaysOpen by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val ivory = Color(0xFFFFE8B0)
    val gold = Color(0xFFE8CA8B)
    val brightGold = Color(0xFFFFE2A3)
    val copper = Color(0xFF8E5424)
    val deepCopper = Color(0xFF211007)
    val ink = Color(0xFF050302)
    val displayMonthNameSet = selectedMonthNameSet(monthNameSetId)
    val textButtonColors = ButtonDefaults.textButtonColors(contentColor = brightGold)
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = copper,
        contentColor = ivory,
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ivory,
        unfocusedTextColor = ivory,
        focusedLabelColor = brightGold,
        unfocusedLabelColor = gold.copy(alpha = 0.82f),
        cursorColor = brightGold,
        focusedBorderColor = brightGold,
        unfocusedBorderColor = gold.copy(alpha = 0.58f),
        focusedContainerColor = ink.copy(alpha = 0.72f),
        unfocusedContainerColor = deepCopper.copy(alpha = 0.58f),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.0f to Color(0xFF3A1D0C),
                    0.48f to Color(0xFF160B05),
                    1.0f to Color(0xFF050302),
                )
            )
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("SETTINGS", color = ivory, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss, colors = textButtonColors) { Text("Close") }
            }

            Text("Calendar", color = gold, style = MaterialTheme.typography.titleMedium)
            ObserverLocationPanel(
                current = observerLocation,
                textColor = ivory,
                accentColor = gold,
                onChanged = onObserverLocationChanged,
            )
            CycleIdCriterion(
                label = "Month Names",
                selectedId = monthNameSetId,
                options = CalendarCatalog.monthNameSets.map { it.id to it.displayName },
                onValue = onMonthNameSetChanged,
            )
            Text("Notifications", color = gold, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Important-day notifications", color = ivory)
                    Text("Quiet status-bar reminders; no sound or vibration. Amavasya is included.", color = ivory.copy(alpha = 0.68f))
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ivory,
                        checkedTrackColor = copper,
                        checkedBorderColor = brightGold,
                        uncheckedThumbColor = gold,
                        uncheckedTrackColor = deepCopper,
                        uncheckedBorderColor = gold.copy(alpha = 0.7f),
                    ),
                )
            }
            CycleIdCriterion(
                label = "New Year",
                selectedId = calendarLocaleRuleId,
                options = CalendarCatalog.calendarLocaleRules.map { it.id to it.displayName },
                onValue = onCalendarLocaleRuleChanged,
            )
            CycleIdCriterion(
                label = "Month System",
                selectedId = monthReckoningId,
                options = MonthReckoning.values().map { it.id to it.displayName },
                onValue = onMonthReckoningChanged,
            )

            Button(
                onClick = { savedDaysOpen = !savedDaysOpen },
                colors = buttonColors,
            ) {
                Text(if (savedDaysOpen) "Hide Your Days" else "See Your Days")
            }
            if (savedDaysOpen) {
                SavedDaysList(
                    events = events,
                    specialDays = specialDays,
                    textColor = ivory,
                    accentColor = brightGold,
                    textButtonColors = textButtonColors,
                    monthNameSet = displayMonthNameSet,
                    onEventsChanged = onEventsChanged,
                    onSpecialDaysChanged = onSpecialDaysChanged,
                    onMessage = { message = it },
                )
            }

            Text("Logo", color = gold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp),
                    )
                }
                Button(onClick = { picker.launch("image/*") }, colors = buttonColors) { Text(if (logoPath == null) "Add Logo" else "Change Logo") }
                if (logoPath != null) {
                    TextButton(onClick = onLogoCleared, colors = textButtonColors) { Text("Remove") }
                }
            }

            Text("User Events", color = gold, style = MaterialTheme.typography.titleMedium)
            if (events.isNotEmpty() && selectedIndex !in events.indices) {
                TextButton(
                    onClick = { selectedIndex = 0 },
                    colors = textButtonColors,
                ) {
                    Text("Edit Saved Event")
                }
            }
            if (selectedIndex in events.indices) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (events.size > 1) {
                        TextButton(onClick = { selectedIndex = (selectedIndex - 1).floorMod(events.size) }, colors = textButtonColors) { Text("Previous") }
                    }
                    Text("${selectedIndex + 1} / ${events.size}", color = ivory)
                    if (events.size > 1) {
                        TextButton(onClick = { selectedIndex = (selectedIndex + 1).floorMod(events.size) }, colors = textButtonColors) { Text("Next") }
                    }
                }
            }
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text("Event name") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            CycleOptionalIdCriterion(
                label = "Maasa",
                selectedId = draft.month,
                options = CalendarCatalog.lunarMonths.map { month ->
                    month.name to localizedMonthName(month.index, displayMonthNameSet)
                },
            ) {
                draft = draft.copy(month = it)
            }
            CycleCriterion("Paksha", draft.paksha, listOf(null, "Shukla", "Krishna")) {
                draft = draft.copy(paksha = it)
            }
            CycleCriterion("Tithi", draft.tithiIndex?.let { CalendarCatalog.tithis[it].name }, listOf(null) + CalendarCatalog.tithis.map { it.name }) { value ->
                draft = draft.copy(tithiIndex = value?.let { CalendarCatalog.tithis.indexOfFirst { tithi -> tithi.name == it } }?.takeIf { it >= 0 })
            }
            CycleCriterion("Nakshatra", draft.nakshatra, listOf(null) + CalendarCatalog.nakshatras.map { it.name }) {
                draft = draft.copy(nakshatra = it)
            }
            CycleCriterion("Rashi", draft.rashi, listOf(null) + CalendarCatalog.rashis.map { it.name }) {
                draft = draft.copy(rashi = it)
            }
            Text("Select at least two criteria.", color = ivory.copy(alpha = 0.72f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val event = draft.toEvent()
                        if (event == null) {
                            message = "Name and at least two criteria are required."
                            return@Button
                        }
                        val next = events.toMutableList()
                        if (selectedIndex in next.indices) {
                            next[selectedIndex] = event
                        } else {
                            next += event
                            selectedIndex = next.lastIndex
                        }
                        onEventsChanged(next)
                        message = "Event saved."
                    },
                    colors = buttonColors,
                ) {
                    Text(if (selectedIndex in events.indices) "Save Event" else "Add Event")
                }
                TextButton(
                    onClick = {
                        selectedIndex = -1
                        draft = UserEvent().toDraft()
                    },
                    colors = textButtonColors,
                ) {
                    Text("New")
                }
                if (selectedIndex in events.indices) {
                    TextButton(
                        onClick = {
                            val next = events.toMutableList().also { it.removeAt(selectedIndex) }
                            onEventsChanged(next)
                            selectedIndex = if (next.isEmpty()) -1 else selectedIndex.coerceAtMost(next.lastIndex)
                            message = "Event deleted."
                        },
                        colors = textButtonColors,
                    ) {
                        Text("Delete")
                    }
                }
            }

            Text("Import JSON / CSV / TSV", color = gold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("Paste events") },
                minLines = 4,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val imported = parseUserEvents(importText)
                    if (imported.isEmpty()) {
                        message = "No valid events found. Include name plus at least two criteria."
                    } else {
                        val next = (events + imported).distinctBy { it.identityKey() }
                        onEventsChanged(next)
                        selectedIndex = next.lastIndex
                        importText = ""
                        message = "Imported ${imported.size} event(s)."
                    }
                },
                colors = buttonColors,
            ) {
                Text("Import Events")
            }
            message?.let { Text(it, color = ivory) }

            Text("About", color = gold, style = MaterialTheme.typography.titleMedium)
            Text("Yantra is free software licensed under AGPL-3.0.", color = ivory.copy(alpha = 0.86f))
            Button(
                onClick = { openRepository(context) },
                colors = buttonColors,
            ) {
                Text("Open Source Repository")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SavedDaysList(
    events: List<UserEvent>,
    specialDays: List<SpecialDay>,
    textColor: Color,
    accentColor: Color,
    textButtonColors: androidx.compose.material3.ButtonColors,
    monthNameSet: MonthNameSet,
    onEventsChanged: (List<UserEvent>) -> Unit,
    onSpecialDaysChanged: (List<SpecialDay>) -> Unit,
    onMessage: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Your Days", color = accentColor, style = MaterialTheme.typography.titleMedium)
        if (specialDays.isEmpty() && events.isEmpty()) {
            Text("None saved", color = textColor.copy(alpha = 0.72f))
            return@Column
        }
        specialDays.forEach { day ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "${day.name} - ${localizedMonthName(day.month, monthNameSet)} ${day.paksha} ${day.tithiNumber}",
                    color = textColor,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        onSpecialDaysChanged(specialDays.filterNot { it == day })
                        onMessage("Deleted ${day.name}.")
                    },
                    colors = textButtonColors,
                ) {
                    Text("Delete")
                }
            }
        }
        events.forEach { event ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "${event.name} - ${event.criteriaLabel(monthNameSet)}",
                    color = textColor,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        onEventsChanged(events.filterNot { it == event })
                        onMessage("Deleted ${event.name}.")
                    },
                    colors = textButtonColors,
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun CycleIdCriterion(
    label: String,
    selectedId: String,
    options: List<Pair<String, String>>,
    onValue: (String) -> Unit,
) {
    if (options.isEmpty()) return
    val current = options.indexOfFirst { it.first == selectedId }.takeIf { it >= 0 } ?: 0
    val textButtonColors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFE2A3))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, color = Color(0xFFFFE8B0), modifier = Modifier.width(116.dp))
        TextButton(onClick = { onValue(options[(current - 1).floorMod(options.size)].first) }, colors = textButtonColors) { Text("-") }
        Text(options[current].second, color = Color(0xFFFFE8B0), modifier = Modifier.weight(1f))
        TextButton(onClick = { onValue(options[(current + 1).floorMod(options.size)].first) }, colors = textButtonColors) { Text("+") }
    }
}

@Composable
private fun CycleOptionalIdCriterion(
    label: String,
    selectedId: String?,
    options: List<Pair<String, String>>,
    onValue: (String?) -> Unit,
) {
    val allOptions = listOf(null to "Any") + options.map { (id, displayName) -> id as String? to displayName }
    val current = allOptions.indexOfFirst { it.first == selectedId }.takeIf { it >= 0 } ?: 0
    val textButtonColors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFE2A3))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, color = Color(0xFFFFE8B0), modifier = Modifier.width(92.dp))
        TextButton(onClick = { onValue(allOptions[(current - 1).floorMod(allOptions.size)].first) }, colors = textButtonColors) { Text("-") }
        Text(allOptions[current].second, color = Color(0xFFFFE8B0), modifier = Modifier.weight(1f))
        TextButton(onClick = { onValue(allOptions[(current + 1).floorMod(allOptions.size)].first) }, colors = textButtonColors) { Text("+") }
    }
}

@Composable
private fun CycleCriterion(
    label: String,
    value: String?,
    options: List<String?>,
    onValue: (String?) -> Unit,
) {
    val current = options.indexOf(value).takeIf { it >= 0 } ?: 0
    val textButtonColors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFE2A3))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, color = Color(0xFFFFE8B0), modifier = Modifier.width(92.dp))
        TextButton(onClick = { onValue(options[(current - 1).floorMod(options.size)]) }, colors = textButtonColors) { Text("-") }
        Text(value ?: "Any", color = Color(0xFFFFE8B0), modifier = Modifier.weight(1f))
        TextButton(onClick = { onValue(options[(current + 1).floorMod(options.size)]) }, colors = textButtonColors) { Text("+") }
    }
}

internal data class UserEvent(
    val name: String = "",
    val month: String? = null,
    val paksha: String? = null,
    val tithiIndex: Int? = null,
    val nakshatra: String? = null,
    val rashi: String? = null,
) {
    fun matches(state: YantraState): Boolean =
        criteriaCount() >= 2 &&
            (month == null || month == state.lunarMonth) &&
            (paksha == null || paksha == state.paksha) &&
            (tithiIndex == null || tithiIndex == state.tithi.index) &&
            (nakshatra == null || nakshatra == state.nakshatra.name) &&
            (rashi == null || rashi == state.solarRashi.name || rashi == state.lunarRashi.name)

    fun criteriaCount(): Int = listOf(month, paksha, tithiIndex, nakshatra, rashi).count { it != null }

    fun identityKey(): String = listOf(name, month, paksha, tithiIndex?.toString(), nakshatra, rashi).joinToString("|")

    fun criteriaLabel(monthNameSet: MonthNameSet): String = listOfNotNull(
        month?.let { "Maasa ${localizedMonthName(it, monthNameSet)}" },
        paksha,
        tithiIndex?.let { CalendarCatalog.tithis[it].name },
        nakshatra?.let { "Nakshatra $it" },
        rashi?.let { "Rashi $it" },
    ).joinToString(", ")

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        month?.let { put("month", it) }
        paksha?.let { put("paksha", it) }
        tithiIndex?.let { put("tithiIndex", it) }
        nakshatra?.let { put("nakshatra", it) }
        rashi?.let { put("rashi", it) }
    }

    fun toDraft(): UserEventDraft = UserEventDraft(name, month, paksha, tithiIndex, nakshatra, rashi)
}

internal data class UserEventDraft(
    val name: String = "",
    val month: String? = null,
    val paksha: String? = null,
    val tithiIndex: Int? = null,
    val nakshatra: String? = null,
    val rashi: String? = null,
) {
    fun toEvent(): UserEvent? {
        val event = UserEvent(name.trim(), month, paksha, tithiIndex, nakshatra, rashi)
        return event.takeIf { it.name.isNotBlank() && it.criteriaCount() >= 2 }
    }
}

private const val USER_SETTINGS_PREFS = "yantra_user_settings"
private const val USER_EVENTS_KEY = "events_json"
private const val USER_LOGO_KEY = "logo_path"
private const val USER_LOGO_FILE = "user_logo"
private const val MONTH_NAME_SET_KEY = "month_name_set"
private const val CALENDAR_LOCALE_RULE_KEY = "calendar_locale_rule"
private const val MONTH_RECKONING_KEY = "month_reckoning"

private fun selectedMonthNameSet(id: String): MonthNameSet =
    CalendarCatalog.monthNameSets.firstOrNull { it.id == id } ?: CalendarCatalog.monthNameSets.first()

private fun selectedCalendarLocaleRule(id: String) =
    CalendarCatalog.calendarLocaleRules.firstOrNull { it.id == id } ?: CalendarCatalog.calendarLocaleRules.first()

private fun selectedMonthReckoning(id: String): MonthReckoning =
    MonthReckoning.values().firstOrNull { it.id == id } ?: MonthReckoning.Amanta

private fun loadMonthNameSetId(context: Context): String =
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getString(MONTH_NAME_SET_KEY, CalendarCatalog.monthNameSets.first().id)
        ?: CalendarCatalog.monthNameSets.first().id

private fun saveMonthNameSetId(context: Context, id: String) {
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(MONTH_NAME_SET_KEY, id)
        .apply()
}

private fun loadCalendarLocaleRuleId(context: Context): String =
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getString(CALENDAR_LOCALE_RULE_KEY, CalendarCatalog.calendarLocaleRules.first().id)
        ?: CalendarCatalog.calendarLocaleRules.first().id

private fun saveCalendarLocaleRuleId(context: Context, id: String) {
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(CALENDAR_LOCALE_RULE_KEY, id)
        .apply()
}

private fun loadMonthReckoningId(context: Context): String =
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getString(MONTH_RECKONING_KEY, MonthReckoning.Amanta.id)
        ?: MonthReckoning.Amanta.id

private fun saveMonthReckoningId(context: Context, id: String) {
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(MONTH_RECKONING_KEY, id)
        .apply()
}

internal fun loadUserEvents(context: Context): List<UserEvent> {
    val raw = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE).getString(USER_EVENTS_KEY, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index -> userEventFromJson(array.getJSONObject(index)) }
            .filter { it.name.isNotBlank() && it.criteriaCount() >= 2 }
    }.getOrDefault(emptyList())
}

private fun saveUserEvents(context: Context, events: List<UserEvent>) {
    val array = JSONArray()
    events.forEach { array.put(it.toJson()) }
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(USER_EVENTS_KEY, array.toString())
        .apply()
}

private fun userEventFromJson(json: JSONObject): UserEvent =
    UserEvent(
        name = json.optString("name").trim(),
        month = canonical(json.optNullableString("month") ?: json.optNullableString("maasa"), CalendarCatalog.lunarMonths.map { it.name }),
        paksha = canonical(json.optNullableString("paksha"), listOf("Shukla", "Krishna")),
        tithiIndex = json.optTithiIndex(),
        nakshatra = canonical(json.optNullableString("nakshatra") ?: json.optNullableString("naksatra"), CalendarCatalog.nakshatras.map { it.name }),
        rashi = canonical(json.optNullableString("rashi"), CalendarCatalog.rashis.map { it.name }),
    )

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).trim().takeIf { it.isNotBlank() && !it.equals("any", true) } else null

private fun JSONObject.optTithiIndex(): Int? {
    if (has("tithiIndex") && !isNull("tithiIndex")) return optInt("tithiIndex").takeIf { it in 0..29 }
    val paksha = optNullableString("paksha")
    val raw = optNullableString("tithi") ?: optNullableString("tithiNumber") ?: return null
    return parseTithiIndex(raw, paksha)
}

private fun parseUserEvents(raw: String): List<UserEvent> {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return emptyList()
    return if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
        parseUserEventsJson(trimmed)
    } else {
        parseUserEventsDelimited(trimmed)
    }.filter { it.name.isNotBlank() && it.criteriaCount() >= 2 }
}

private fun parseUserEventsJson(raw: String): List<UserEvent> =
    runCatching {
        val array = if (raw.startsWith("[")) JSONArray(raw) else JSONArray().put(JSONObject(raw))
        List(array.length()) { index -> userEventFromJson(array.getJSONObject(index)) }
    }.getOrDefault(emptyList())

private fun parseUserEventsDelimited(raw: String): List<UserEvent> {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.size < 2) return emptyList()
    val delimiter = if (lines.first().contains('\t')) '\t' else ','
    val headers = lines.first().split(delimiter).map { it.trim().lowercase() }
    return lines.drop(1).mapNotNull { line ->
        val values = line.split(delimiter).map { it.trim() }
        val map = headers.mapIndexedNotNull { index, header -> values.getOrNull(index)?.let { header to it } }.toMap()
        val paksha = canonical(map["paksha"].normalizeCriterion(), listOf("Shukla", "Krishna"))
        UserEvent(
            name = map["name"].orEmpty().trim(),
            month = canonical((map["month"] ?: map["maasa"]).normalizeCriterion(), CalendarCatalog.lunarMonths.map { it.name }),
            paksha = paksha,
            tithiIndex = (map["tithiindex"]?.toIntOrNull()?.takeIf { it in 0..29 })
                ?: parseTithiIndex(map["tithi"] ?: map["tithinumber"], paksha),
            nakshatra = canonical((map["nakshatra"] ?: map["naksatra"]).normalizeCriterion(), CalendarCatalog.nakshatras.map { it.name }),
            rashi = canonical(map["rashi"].normalizeCriterion(), CalendarCatalog.rashis.map { it.name }),
        )
    }
}

private fun String?.normalizeCriterion(): String? =
    this?.trim()?.takeIf { it.isNotBlank() && !it.equals("any", true) }

private fun canonical(value: String?, options: List<String>): String? =
    value?.let { candidate -> options.firstOrNull { it.equals(candidate, true) } }

private fun parseTithiIndex(raw: String?, paksha: String?): Int? {
    val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    CalendarCatalog.tithis.indexOfFirst { it.name.equals(value, true) }.takeIf { it >= 0 }?.let { return it }
    val number = value.filter { it.isDigit() }.toIntOrNull()?.takeIf { it in 1..30 } ?: return null
    if (number > 15) return number - 1
    return when (paksha?.lowercase()) {
        "krishna" -> number + 14
        else -> number - 1
    }
}

private fun loadUserLogoPath(context: Context): String? =
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getString(USER_LOGO_KEY, null)
        ?.takeIf { File(it).exists() }

private fun saveUserLogoPath(context: Context, path: String?) {
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(USER_LOGO_KEY, path)
        .apply()
}

private fun saveUserLogo(context: Context, uri: Uri): String {
    val destination = File(context.filesDir, USER_LOGO_FILE)
    context.contentResolver.openInputStream(uri)?.use { input ->
        destination.outputStream().use { output -> input.copyTo(output) }
    }
    return destination.absolutePath
}

private fun clearUserLogo(context: Context) {
    File(context.filesDir, USER_LOGO_FILE).delete()
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .remove(USER_LOGO_KEY)
        .apply()
}

private fun loadLogoBitmap(path: String): ImageBitmap? =
    runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()

private const val YANTRA_REPOSITORY_URL = "https://github.com/octotus/Yantra"

private fun openRepository(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YANTRA_REPOSITORY_URL))
    runCatching { context.startActivity(intent) }
}

private fun Int.floorMod(modulus: Int): Int = Math.floorMod(this, modulus)

internal object FestivalCatalog {
    internal val definitions = listOf(
        FestivalDefinition("Makara Sankranti", FestivalRank.Major, solarRashi = "Makara", previousSolarRashi = "Dhanu", astronomicalDefinition = "Solar ingress into sidereal Makara."),
        FestivalDefinition("Chaitra New Year", FestivalRank.Major, month = "Chaitra", paksha = "Shukla", tithiNumber = 1, astronomicalDefinition = "Chaitra Shukla Pratipada."),
        FestivalDefinition("Rama Navami", FestivalRank.Major, month = "Chaitra", paksha = "Shukla", tithiNumber = 9, astronomicalDefinition = "Chaitra Shukla Navami."),
        FestivalDefinition("Akshaya Tritiya", FestivalRank.Major, month = "Vaishakha", paksha = "Shukla", tithiNumber = 3, astronomicalDefinition = "Vaishakha Shukla Tritiya."),
        FestivalDefinition("Krishna Janmashtami", FestivalRank.Major, month = "Bhadrapada", paksha = "Krishna", tithiNumber = 8, astronomicalDefinition = "Bhadrapada Krishna Ashtami."),
        FestivalDefinition("Navaratri Begins", FestivalRank.Major, month = "Ashwin", paksha = "Shukla", tithiNumber = 1, astronomicalDefinition = "Ashwin Shukla Pratipada."),
        FestivalDefinition("Vijayadashami", FestivalRank.Major, month = "Ashwin", paksha = "Shukla", tithiNumber = 10, astronomicalDefinition = "Ashwin Shukla Dashami."),
        FestivalDefinition("Diwali", FestivalRank.Major, month = "Kartika", paksha = "Krishna", tithiNumber = 15, astronomicalDefinition = "Kartika Krishna Amavasya."),
        FestivalDefinition("Karthika Deepam", FestivalRank.Major, month = "Kartika", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Kartika Shukla Purnima, traditionally associated with Krittika nakshatra."),
        FestivalDefinition("Holi", FestivalRank.Minor, month = "Phalguna", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Phalguna Shukla Purnima."),
        FestivalDefinition("Thiruvadirai", FestivalRank.Major, solarRashi = "Dhanu", nakshatra = "Ardra", astronomicalDefinition = "Tamil Margazhi / solar Dhanu when Ardra (Thiruvathirai) nakshatra prevails, traditionally on or near the full moon night."),
        FestivalDefinition("Vaikuntha Ekadashi", FestivalRank.Major, solarRashi = "Dhanu", paksha = "Shukla", tithiNumber = 11, astronomicalDefinition = "Solar Dhanu masa, Shukla Paksha, Ekadashi tithi."),
        FestivalDefinition("Maha Shivaratri", FestivalRank.Major, month = "Phalguna", paksha = "Krishna", tithiNumber = 14, astronomicalDefinition = "Phalguna Krishna Chaturdashi."),
        FestivalDefinition("Hanuman Jayanti", FestivalRank.Minor, month = "Chaitra", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Chaitra Shukla Purnima."),
        FestivalDefinition("Guru Purnima", FestivalRank.Minor, month = "Ashadha", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Ashadha Shukla Purnima."),
        FestivalDefinition("Nag Panchami", FestivalRank.Minor, month = "Shravana", paksha = "Shukla", tithiNumber = 5, astronomicalDefinition = "Shravana Shukla Panchami."),
        FestivalDefinition("Upakarma", FestivalRank.Major, month = "Shravana", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Shravana Shukla Purnima."),
        FestivalDefinition("Ganesh Chaturthi", FestivalRank.Major, month = "Bhadrapada", paksha = "Shukla", tithiNumber = 4, astronomicalDefinition = "Bhadrapada Shukla Chaturthi."),
        FestivalDefinition("Vasant Panchami", FestivalRank.Minor, month = "Magha", paksha = "Shukla", tithiNumber = 5, astronomicalDefinition = "Magha Shukla Panchami."),
        FestivalDefinition("Gita Jayanti", FestivalRank.Minor, month = "Margashirsha", paksha = "Shukla", tithiNumber = 11, astronomicalDefinition = "Margashirsha Shukla Ekadashi."),
        FestivalDefinition("Ekadashi", FestivalRank.Minor, tithiNumber = 11, astronomicalDefinition = "Ekadashi tithi in either paksha."),
        FestivalDefinition("Pradosham", FestivalRank.Minor, tithiNumber = 13, astronomicalDefinition = "Trayodashi tithi in either paksha."),
    )

    fun match(state: YantraState, previousState: YantraState): FestivalDefinition? {
        val tithiNumber = (state.tithi.index % 15) + 1
        return definitions.firstOrNull { festival -> matches(festival, state, previousState, tithiNumber) }
    }

    internal fun matches(festival: FestivalDefinition, state: YantraState, previousState: YantraState): Boolean =
        matches(festival, state, previousState, (state.tithi.index % 15) + 1)

    private fun matches(festival: FestivalDefinition, state: YantraState, previousState: YantraState, tithiNumber: Int): Boolean {
        val expectedMonth = festivalMonth(festival, state.monthReckoning)
        return (expectedMonth == null || expectedMonth == state.lunarMonth) &&
            (festival.paksha == null || festival.paksha == state.paksha) &&
            (festival.tithiNumber == null || festival.tithiNumber == tithiNumber) &&
            (festival.solarRashi == null || festival.solarRashi == state.solarRashi.name) &&
            (festival.previousSolarRashi == null || festival.previousSolarRashi == previousState.solarRashi.name) &&
            (festival.nakshatra == null || festival.nakshatra == state.nakshatra.name)
    }

    internal fun festivalMonth(festival: FestivalDefinition, reckoning: MonthReckoning): String? {
        val month = festival.month ?: return null
        if (reckoning != MonthReckoning.Amanta || festival.paksha != "Krishna") return month
        val index = CalendarCatalog.lunarMonths.indexOfFirst { it.name == month }
        return if (index < 0) month else CalendarCatalog.lunarMonths[Math.floorMod(index - 1, 12)].name
    }
}

private fun cryptexLayout(width: Float, height: Float): CryptexLayout {
    val panelWidth = min(width * 0.86f, height * 0.62f)
    val panelHeight = panelWidth * 0.58f
    val panelCenter = Offset(width / 2f, height / 2f)
    val outerRect = Rect(
        panelCenter.x - panelWidth / 2f,
        panelCenter.y - panelHeight / 2f,
        panelCenter.x + panelWidth / 2f,
        panelCenter.y + panelHeight / 2f,
    )
    val gap = panelWidth * 0.035f
    val columnWidth = (panelWidth - gap * 4f) / 3f
    val columnTop = outerRect.top + panelHeight * 0.17f
    val columnHeight = panelHeight * 0.58f
    val columns = List(3) { index ->
        val left = outerRect.left + gap + index * (columnWidth + gap)
        Rect(left, columnTop, left + columnWidth, columnTop + columnHeight)
    }
    val commitRect = Rect(
        outerRect.left + panelWidth * 0.18f,
        outerRect.bottom - panelHeight * 0.18f,
        outerRect.right - panelWidth * 0.18f,
        outerRect.bottom - panelHeight * 0.05f,
    )
    return CryptexLayout(outerRect, columns, commitRect)
}

private data class YantraLayout(
    val center: Offset,
    val radius: Float,
    val dateHitRect: Rect,
    val settingsHitRect: Rect,
    val backHitRect: Rect,
    val lotusCenter: Offset,
    val lotusRadius: Float,
)

private enum class AnnotationKind {
    Rashi,
    Masa,
    Nakshatra,
    Tithi,
}

private data class YantraAnnotation(
    val kind: AnnotationKind,
    val index: Int,
    val name: String,
    val durationLabel: String? = null,
)

private fun yantraLayout(width: Float, height: Float): YantraLayout {
    val radius = min(width * 0.462f, height * 0.34f)
    val ringWidth = radius * 0.135f
    val tithiRingWidth = ringWidth * 1.15f
    val tithiRingRadius = radius - ringWidth * 0.35f
    val faceRadius = tithiRingRadius + tithiRingWidth * 0.42f
    val bodyRadius = faceRadius + ringWidth * 0.38f
    val topTextOffset = ringWidth * 0.48f
    val bottomTextOffset = ringWidth * 1.72f + lotusRadiusForLayout(ringWidth) + radius * 0.2592f
    val centerY = min(height * 0.47f, height - bodyRadius - bottomTextOffset - ringWidth * 1.1f)
        .coerceAtLeast(bodyRadius + topTextOffset + ringWidth * 0.65f)
    val center = Offset(width / 2f, centerY)
    val lotusCenter = Offset(center.x, center.y + bodyRadius + ringWidth * 1.72f)
    val lotusRadius = lotusRadiusForLayout(ringWidth)
    val dateCenter = Offset(center.x, center.y + bodyRadius + bottomTextOffset)
    val dateHitRect = Rect(
        dateCenter.x - min(width * 0.44f, radius * 0.95f),
        dateCenter.y - ringWidth * 0.62f,
        dateCenter.x + min(width * 0.44f, radius * 0.95f),
        dateCenter.y + ringWidth * 0.62f,
    )
    val gearSize = min(width, height) * 0.0665f
    val gearCenter = Offset(width * 0.09f, height - gearSize * 2.35f)
    val settingsHitRect = Rect(
        gearCenter.x - gearSize * 0.72f,
        gearCenter.y - gearSize * 0.72f,
        gearCenter.x + gearSize * 0.72f,
        gearCenter.y + gearSize * 0.72f,
    )
    val backCenter = Offset(width * 0.91f, gearCenter.y)
    val backHitRect = Rect(
        backCenter.x - gearSize * 0.72f,
        backCenter.y - gearSize * 0.72f,
        backCenter.x + gearSize * 0.72f,
        backCenter.y + gearSize * 0.72f,
    )
    return YantraLayout(center, radius, dateHitRect, settingsHitRect, backHitRect, lotusCenter, lotusRadius)
}

private fun lotusRadiusForLayout(ringWidth: Float): Float = ringWidth * 0.76f

@Composable
private fun YantraInstrument(
    state: YantraState,
    sigilImages: SigilImages,
    monthNameSet: MonthNameSet,
    lunarEmphasis: Boolean,
    festival: FestivalDefinition?,
    observanceLabel: String?,
    showFestivalLabel: Boolean,
    annotation: YantraAnnotation?,
    showBack: Boolean,
    userLogo: ImageBitmap?,
    userLogoLit: Boolean,
    now: ZonedDateTime,
    modifier: Modifier = Modifier,
    onMoonTap: () -> Unit,
    onDateTap: () -> Unit,
    onDateLongPress: () -> Unit,
    onSettingsTap: () -> Unit,
    onBackTap: () -> Unit,
    onFestivalTap: () -> Unit,
    onAnnotation: (YantraAnnotation) -> Unit,
    onAnnotationDismiss: () -> Unit,
) {
    val moonlight by animateFloatAsState(
        targetValue = (state.moonIllumination * ((state.lunarAltitude + 8.0) / 58.0)).toFloat().coerceIn(0f, 0.55f),
        label = "moonlight",
    )
    val animatedIllumination by animateFloatAsState(
        targetValue = state.moonIllumination.toFloat().coerceIn(0f, 1f),
        label = "moon_phase",
    )
    val activeGold = Color(0xFFFFD992)
    val activeSilver = Color(0xFFD7E5FF)
    val bronze = Color(0xFF5E4328)
    val brass = Color(0xFFC59B5C)
    val brightGold = Color(0xFFFFE2A3)
    val dateLabel = remember(now) { now.format(DateTimeFormatter.ofPattern("dd MMM yyyy")).uppercase() }
    val yearLabel = state.samvatsara.name.uppercase()

    Canvas(
        modifier = modifier.pointerInput(state, now, observanceLabel, annotation) {
            detectTapGestures(
                onLongPress = { tap ->
                    val layout = yantraLayout(size.width.toFloat(), size.height.toFloat())
                    if (annotation == null && tap.isInRect(layout.dateHitRect)) {
                        onDateLongPress()
                    }
                },
                onTap = { tap ->
                    val layout = yantraLayout(size.width.toFloat(), size.height.toFloat())
                    if (annotation != null) {
                        val card = annotationCardRect(layout.center, layout.radius, annotation.durationLabel?.lineSequence()?.count() ?: 0)
                        if (tap.isInRect(annotationBackRect(card))) onAnnotationDismiss()
                        return@detectTapGestures
                    }
                    val distance = hypot(tap.x - layout.center.x, tap.y - layout.center.y)
                    val ringWidth = layout.radius * 0.135f
                    val tithiRingRadius = layout.radius - ringWidth * 0.35f
                    val nakshatraRingRadius = layout.radius - ringWidth * 1.5f
                    val monthRingRadius = layout.radius - ringWidth * 2.65f
                    val rashiRingRadius = layout.radius - ringWidth * 3.72f
                    val moonRadius = layout.radius * 0.19f
                    if (distance <= moonRadius * 1.6f) {
                        onMoonTap()
                    } else if (tap.isInRect(layout.settingsHitRect)) {
                        onSettingsTap()
                    } else if (showBack && tap.isInRect(layout.backHitRect)) {
                        onBackTap()
                    } else if (hypot(tap.x - layout.lotusCenter.x, tap.y - layout.lotusCenter.y) <= layout.lotusRadius * 1.35f) {
                        onFestivalTap()
                    } else if (tap.isInRect(layout.dateHitRect)) {
                        onDateTap()
                    } else {
                        hitAnnotation(tap, layout.center, ringWidth, tithiRingRadius, nakshatraRingRadius, monthRingRadius, rashiRingRadius, state, monthNameSet)?.let(onAnnotation)
                    }
                }
            )
        }
    ) {
        val layout = yantraLayout(size.width, size.height)
        val center = layout.center
        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
        val centerShift = Offset(center.x - canvasCenter.x, center.y - canvasCenter.y)
        val radius = layout.radius
        val ringWidth = radius * 0.135f
        val silverGlow = activeSilver.copy(alpha = 0.25f + moonlight * 0.75f)
        val rashiActiveIndex = if (lunarEmphasis) state.lunarRashi.index else state.solarRashi.index
        val rashiActiveColor = if (lunarEmphasis) activeSilver.copy(alpha = 0.96f) else activeGold.copy(alpha = 0.92f)
        val lunarBoost = if (lunarEmphasis) 1.0f else 0.0f
        val tithiRingWidth = ringWidth * 1.15f
        val tithiRingRadius = radius - ringWidth * 0.35f
        val nakshatraRingRadius = radius - ringWidth * 1.5f
        val monthRingRadius = radius - ringWidth * 2.65f
        val rashiRingRadius = radius - ringWidth * 3.72f
        val rashiRingWidth = ringWidth * 1.34f
        val faceRadius = tithiRingRadius + tithiRingWidth * 0.42f
        val bodyRadius = faceRadius + ringWidth * 0.38f
        val lotusLit = observanceLabel != null

        drawPocketWatchBody(center, faceRadius, bodyRadius, ringWidth, brass, brightGold)
        drawDeviceLighting(center, faceRadius, state.solarAltitude.toFloat(), moonlight, brightGold)
        userLogo?.let {
            drawUserLogo(
                image = it,
                center = Offset(size.width / 2f, maxOf(size.height * 0.055f, ringWidth * 1.45f)),
                size = ringWidth * 1.9f,
                lit = userLogoLit,
                gold = brightGold,
            )
        }

        withTransform({
            translate(left = centerShift.x, top = centerShift.y)
        }) {
            drawTithiRing(
                activeIndex = state.tithi.index,
                radius = tithiRingRadius,
                width = tithiRingWidth,
                inactive = bronze,
                active = activeGold,
            )
            drawRing(27, state.nakshatra.index, nakshatraRingRadius, ringWidth, bronze, activeGold.copy(alpha = 0.22f + lunarBoost * 0.08f))
            drawMonthRing(
                sectors = localizedMonthSectors(state.monthSectors, monthNameSet),
                activeIndex = state.month.index,
                radius = monthRingRadius,
                width = ringWidth,
                inactive = bronze,
                active = activeGold,
            )
            drawLagnaRashiRing(
                sectors = state.lagnaSectors,
                activeLagnaIndex = state.lagnaRashi?.index,
                dayFraction = state.lagnaDayFraction,
                radius = rashiRingRadius,
                width = rashiRingWidth,
                inactive = bronze,
                activeLagna = activeGold,
            )
            drawNakshatraSigilRing(
                activeIndex = state.nakshatra.index,
                radius = nakshatraRingRadius,
                iconSize = ringWidth * 1.08f,
                inactive = brass.copy(alpha = 0.4f),
                active = activeGold.copy(alpha = 0.96f),
                images = sigilImages,
            )
            drawRashiSigilRing(
                activeIndex = rashiActiveIndex,
                lagnaIndex = state.lagnaRashi?.index,
                sectors = state.lagnaSectors,
                radius = rashiRingRadius,
                iconSize = ringWidth * 1.2f,
                trackWidth = rashiRingWidth,
                inactive = brass.copy(alpha = 0.46f),
                active = rashiActiveColor,
            )
        }
        drawMetalCircleBoundaries(
            center = center,
            radii = listOf(
                nakshatraRingRadius,
                monthRingRadius,
                rashiRingRadius,
            ),
            width = ringWidth,
            color = brightGold,
        )
        drawMetalCircleBoundaries(
            center = center,
            radii = listOf(tithiRingRadius),
            width = tithiRingWidth,
            color = brightGold,
        )

        val moonRadius = radius * 0.19f
        drawCircle(Color(0xFF19120B), moonRadius * 1.6f, center)
        drawCircle(silverGlow, moonRadius * 1.25f, center)
        drawMoon(center, moonRadius, animatedIllumination.toDouble(), state.lunarLongitude, state.solarLongitude)
        drawCircle(brightGold.copy(alpha = 0.62f), moonRadius * 1.6f, center, style = Stroke(width = 0.8.dp.toPx()))
        if (festival?.rank == FestivalRank.Major) {
            drawMajorFestivalOverlay(
                festival = festival,
                center = center,
                radius = moonRadius * 1.28f,
                gold = brightGold,
            )
        }
        withTransform({
            translate(left = centerShift.x, top = centerShift.y)
        }) {
            drawCurvedActiveReadout(
                rashi = if (lunarEmphasis) state.lunarRashi.name else state.solarRashi.name,
                nakshatra = state.nakshatra.name,
                radius = moonRadius * 1.85f,
                textSize = ringWidth * 0.495f * 1.14f,
                nakshatraRadiusOffset = ringWidth * 0.495f * 1.14f * 0.5f,
                nakshatraTextScale = 1.0f,
                text = Color(0xFFE8CA8B),
                accent = if (lunarEmphasis) activeSilver else activeGold,
            )
        }

        drawGlassLayer(center, bodyRadius, innerRadius = faceRadius)
        drawCircumferenceLabels(
            center = center,
            bodyRadius = bodyRadius,
            instrumentRadius = radius,
            ringWidth = ringWidth,
            dateLabel = dateLabel,
            yearLabel = yearLabel,
            gold = brightGold,
        )
        drawFestivalLotus(
            center = layout.lotusCenter,
            size = layout.lotusRadius * 2.0f,
            instrumentRadius = radius,
            lit = lotusLit,
            gold = brightGold,
            label = if (showFestivalLabel) observanceLabel else null,
        )
        annotation?.let {
            drawAnnotationCard(it, sigilImages, center, radius, ringWidth, brightGold)
        }
        drawSettingsGear(layout.settingsHitRect.center, layout.settingsHitRect.width * 0.64f, brightGold)
        if (showBack) drawBackArrowhead(layout.backHitRect.center, layout.backHitRect.width * 0.54f, brightGold)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackArrowhead(
    center: Offset,
    size: Float,
    gold: Color,
    prominent: Boolean = false,
) {
    if (prominent) {
        drawCircle(Color(0xFF211007).copy(alpha = 0.92f), size * 0.82f, center)
        drawCircle(gold.copy(alpha = 0.88f), size * 0.82f, center, style = Stroke(width = size * 0.08f))
    }
    val path = Path().apply {
        moveTo(center.x + size * 0.28f, center.y - size * 0.42f)
        lineTo(center.x - size * 0.22f, center.y)
        lineTo(center.x + size * 0.28f, center.y + size * 0.42f)
    }
    drawPath(
        path,
        gold.copy(alpha = if (prominent) 0.98f else 0.43f),
        style = Stroke(width = size * if (prominent) 0.17f else 0.12f, cap = StrokeCap.Round),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPocketWatchBody(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    width: Float,
    brass: Color,
    gold: Color,
) {
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFF32200F),
            0.68f to Color(0xFF160F08),
            1.0f to Color(0xFF070504),
            center = center,
            radius = innerRadius,
        ),
        radius = innerRadius,
        center = center,
    )
    drawCircle(Color(0xFF080604), outerRadius + width * 0.05f, center, style = Stroke(width = width * 0.1f))
    drawCircle(gold.copy(alpha = 0.86f), innerRadius + width * 0.06f, center, style = Stroke(width = width * 0.06f))
    drawCircle(brass.copy(alpha = 0.7f), innerRadius + width * 0.2f, center, style = Stroke(width = width * 0.18f))
    drawCircle(Color(0xFFFFF0BD).copy(alpha = 0.62f), outerRadius, center, style = Stroke(width = 1.dp.toPx()))
}

private fun localizedMonthSectors(sectors: List<MonthSector>, monthNameSet: MonthNameSet): List<MonthSector> =
    sectors.map { sector ->
        val index = sector.index.coerceIn(0, CalendarCatalog.lunarMonths.lastIndex)
        sector.copy(
            name = monthNameSet.monthNames.getOrElse(index) { sector.name },
            abbreviation = monthNameSet.abbreviations.getOrElse(index) { sector.abbreviation },
        )
    }

private fun localizedMonthName(index: Int, monthNameSet: MonthNameSet): String =
    monthNameSet.monthNames.getOrElse(index.coerceIn(0, CalendarCatalog.lunarMonths.lastIndex)) {
        CalendarCatalog.lunarMonths[index.coerceIn(0, CalendarCatalog.lunarMonths.lastIndex)].name
    }

private fun localizedMonthName(canonicalName: String, monthNameSet: MonthNameSet): String {
    val index = CalendarCatalog.lunarMonths.indexOfFirst { it.name == canonicalName }
    return if (index >= 0) localizedMonthName(index, monthNameSet) else canonicalName
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUserLogo(
    image: ImageBitmap,
    center: Offset,
    size: Float,
    lit: Boolean,
    gold: Color,
) {
    if (lit) {
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to gold.copy(alpha = 0.54f),
                0.58f to gold.copy(alpha = 0.2f),
                1.0f to Color.Transparent,
                center = center,
                radius = size * 0.9f,
            ),
            radius = size,
            center = center,
        )
    }
    val px = size.toInt().coerceAtLeast(1)
    drawCircle(Color(0xFF050403).copy(alpha = 0.72f), size * 0.52f, center)
    drawImage(
        image = image,
        srcOffset = IntOffset(0, 0),
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset((center.x - px / 2f).toInt(), (center.y - px / 2f).toInt()),
        dstSize = IntSize(px, px),
        alpha = if (lit) 1.0f else 0.62f,
    )
    drawCircle(gold.copy(alpha = if (lit) 0.82f else 0.34f), size * 0.52f, center, style = Stroke(width = size * 0.025f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSettingsGear(
    center: Offset,
    size: Float,
    gold: Color,
) {
    val color = Color(0xFFE8CA8B).copy(alpha = 0.43f)
    val radius = size * 0.34f
    drawCircle(Color(0xFF050302).copy(alpha = 0.31f), radius * 1.62f, center)
    drawCircle(gold.copy(alpha = 0.21f), radius * 1.58f, center, style = Stroke(width = size * 0.035f))
    repeat(8) { index ->
        val angle = index * Math.PI.toFloat() / 4f
        drawLine(
            color = color,
            start = center.polar(angle, radius * 1.05f),
            end = center.polar(angle, radius * 1.38f),
            strokeWidth = size * 0.095f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(color, radius, center, style = Stroke(width = size * 0.09f))
    drawCircle(color, radius * 0.36f, center, style = Stroke(width = size * 0.07f))
}

private fun hitAnnotation(
    tap: Offset,
    center: Offset,
    ringWidth: Float,
    tithiRingRadius: Float,
    nakshatraRingRadius: Float,
    monthRingRadius: Float,
    rashiRingRadius: Float,
    state: YantraState,
    monthNameSet: MonthNameSet,
): YantraAnnotation? {
    val distance = hypot(tap.x - center.x, tap.y - center.y)
    return when {
        distance.isInRing(tithiRingRadius, ringWidth * 1.15f) -> {
            val angle = (Math.toDegrees(kotlin.math.atan2((tap.y - center.y).toDouble(), (tap.x - center.x).toDouble())) + 360.0) % 360.0
            val index = (0 until 30).firstOrNull { candidate ->
                normalizePhase(angle - tithiCellStartAngle(candidate)) < 12.0
            } ?: state.tithi.index
            YantraAnnotation(AnnotationKind.Tithi, index, CalendarCatalog.tithis[index].name)
        }
        distance.isInRing(rashiRingRadius, ringWidth) -> {
            val fraction = angleToFraction(tap, center)
            val index = state.lagnaSectors.firstOrNull { sector ->
                fraction >= sector.startFraction && fraction < sector.startFraction + sector.durationFraction
            }?.index ?: angleToIndex(tap, center, 12)
            val rashi = CalendarCatalog.rashis[index]
            YantraAnnotation(AnnotationKind.Rashi, index, rashi.name)
        }
        distance.isInRing(monthRingRadius, ringWidth) -> {
            val fraction = angleToFraction(tap, center)
            val sectors = localizedMonthSectors(state.monthSectors, monthNameSet)
            val sector = sectors.firstOrNull { month ->
                val start = sectors.takeWhile { it.index != month.index }.sumOf { it.arcDegrees } / 360.0
                fraction >= start && fraction < start + month.arcDegrees / 360.0
            } ?: sectors.lastOrNull()
            sector?.let { YantraAnnotation(AnnotationKind.Masa, it.index, it.name) }
        }
        distance.isInRing(nakshatraRingRadius, ringWidth) -> {
            val index = angleToIndex(tap, center, 27)
            val nakshatra = CalendarCatalog.nakshatras[index]
            YantraAnnotation(AnnotationKind.Nakshatra, index, nakshatra.name)
        }
        else -> null
    }
}

private fun Float.isInRing(radius: Float, width: Float): Boolean =
    this >= radius - width * 0.56f && this <= radius + width * 0.56f

private fun angleToFraction(tap: Offset, center: Offset): Double {
    val angle = Math.toDegrees(kotlin.math.atan2((tap.y - center.y).toDouble(), (tap.x - center.x).toDouble()))
    return ((angle + 90.0 + 360.0) % 360.0) / 360.0
}

private fun angleToIndex(tap: Offset, center: Offset, count: Int): Int =
    floor(angleToFraction(tap, center) * count).toInt().coerceIn(0, count - 1)

private fun annotationCardRect(center: Offset, radius: Float, detailLineCount: Int): Rect {
    val width = radius * 1.22f
    val height = radius * when {
        detailLineCount > 6 -> 1.58f
        detailLineCount > 4 -> 1.36f
        detailLineCount > 0 -> 0.92f
        else -> 0.72f
    }
    return Rect(center.x - width / 2f, center.y - height / 2f, center.x + width / 2f, center.y + height / 2f)
}

private fun annotationBackRect(card: Rect): Rect {
    val size = card.width * 0.17f
    return Rect(card.left, card.top, card.left + size, card.top + size)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnnotationCard(
    annotation: YantraAnnotation,
    sigilImages: SigilImages,
    center: Offset,
    radius: Float,
    ringWidth: Float,
    gold: Color,
) {
    val detailLines = annotation.durationLabel?.lineSequence()?.toList().orEmpty()
    val rect = annotationCardRect(center, radius, detailLines.size)
    val cardHeight = rect.height
    drawRoundRect(
        color = Color(0xFF080604).copy(alpha = 0.86f),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(cardHeight * 0.065f, cardHeight * 0.065f),
    )
    drawRoundRect(
        color = gold.copy(alpha = 0.58f),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(cardHeight * 0.065f, cardHeight * 0.065f),
        style = Stroke(width = 0.8.dp.toPx()),
    )
    val backRect = annotationBackRect(rect)
    drawBackArrowhead(backRect.center, backRect.width * 0.38f, gold, prominent = true)
    val denseRashi = detailLines.size > 6
    val sigilCenter = Offset(rect.center.x, rect.top + cardHeight * if (denseRashi) 0.12f else if (detailLines.size > 4) 0.17f else 0.24f)
    val sigilSize = cardHeight * if (denseRashi) 0.14f else if (detailLines.size > 4) 0.19f else 0.27f
    when (annotation.kind) {
        AnnotationKind.Rashi -> drawRashiSigil(annotation.index, sigilCenter, sigilSize, gold)
        AnnotationKind.Nakshatra -> drawNakshatraSigil(annotation.index, sigilCenter, sigilSize, gold, sigilImages)
        AnnotationKind.Tithi -> Unit
        AnnotationKind.Masa -> drawIntoCanvas { canvas ->
            drawEmbossedText(
                native = canvas.nativeCanvas,
                text = annotation.name.take(3).uppercase(),
                x = sigilCenter.x,
                y = sigilCenter.y + sigilSize * 0.13f,
                size = sigilSize * 0.34f,
                color = gold,
                bold = true,
            )
        }
    }
    drawIntoCanvas { canvas ->
        drawEmbossedText(
            native = canvas.nativeCanvas,
            text = annotation.name.uppercase(),
            x = rect.center.x,
            y = if (annotation.kind == AnnotationKind.Tithi) rect.top + cardHeight * 0.30f else if (detailLines.isEmpty()) rect.top + cardHeight * 0.72f else rect.top + cardHeight * if (denseRashi) 0.23f else if (detailLines.size > 4) 0.31f else 0.48f,
            size = cardHeight * if (denseRashi) 0.062f else if (detailLines.size > 4) 0.075f else 0.10f,
            color = Color(0xFFFFE8B0),
            bold = true,
        )
    }
    detailLines.forEachIndexed { index, line ->
        drawIntoCanvas { canvas ->
            drawEmbossedText(
                native = canvas.nativeCanvas,
                text = line,
                x = rect.center.x,
                y = rect.top + cardHeight * if (annotation.kind == AnnotationKind.Tithi) (0.55f + index * 0.17f) else if (denseRashi) (0.34f + index * 0.068f) else if (detailLines.size > 4) (0.43f + index * 0.095f) else (0.68f + index * 0.14f),
                size = cardHeight * if (denseRashi) 0.048f else if (detailLines.size > 4) 0.058f else 0.075f,
                color = if (denseRashi && index % 3 == 0) Color(0xFFFFE8B0) else Color(0xFFE8CA8B).copy(alpha = 0.9f),
                bold = denseRashi && index % 3 == 0,
            )
        }
    }
}

private fun YantraAnnotation.withDuration(
    context: android.content.Context,
    now: ZonedDateTime,
    state: YantraState,
    engine: YantraCalendarEngine,
    observer: Observer,
): YantraAnnotation {
    if (kind == AnnotationKind.Rashi) {
        val timeFormatter = DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a")
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val dateTimeFormatter = DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(context)) "MMM d, HH:mm" else "MMM d, h:mm a")
        val details = mutableListOf<String>()
        state.lagnaSectors.firstOrNull { it.index == index }?.let { sector ->
            val day = now.toLocalDate().atStartOfDay(now.zone)
            val interval = day.plusSeconds((sector.startFraction * 86_400).toLong()) to
                day.plusSeconds(((sector.startFraction + sector.durationFraction) * 86_400).toLong())
            details += "Lagna - Today"
            details += "From: ${interval.first.format(timeFormatter)}"
            details += "To: ${interval.second.format(timeFormatter)}"
        }
        engine.rashiInterval(now, observer, index, solar = true)?.let { interval ->
            details += "SURYA"
            details += "From: ${interval.first.format(dateFormatter)}"
            details += "To: ${interval.second.format(dateFormatter)}"
        }
        engine.rashiInterval(now, observer, index, solar = false)?.let { interval ->
            details += "Chandra"
            details += "From: ${interval.first.format(dateTimeFormatter)}"
            details += "To: ${interval.second.format(dateTimeFormatter)}"
        }
        return if (details.isEmpty()) this else copy(durationLabel = details.joinToString("\n"))
    }
    val interval = when (kind) {
        AnnotationKind.Nakshatra -> engine.nakshatraInterval(now, observer, index)
        AnnotationKind.Tithi -> engine.tithiInterval(now, observer, index)
        AnnotationKind.Masa -> null
        AnnotationKind.Rashi -> null
    } ?: return this
    val timeFormatter = DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a")
    val sameDay = interval.first.toLocalDate() == interval.second.toLocalDate()
    val label = if (sameDay) {
        "From ${interval.first.format(timeFormatter)}\nTo ${interval.second.format(timeFormatter)}"
    } else {
        val dateTime = DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(context)) "MMM d, HH:mm" else "MMM d, h:mm a")
        "From ${interval.first.format(dateTime)}\nTo ${interval.second.format(dateTime)}"
    }
    return copy(durationLabel = label)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCircumferenceLabels(
    center: Offset,
    bodyRadius: Float,
    instrumentRadius: Float,
    ringWidth: Float,
    dateLabel: String,
    yearLabel: String,
    gold: Color,
) {
    val topRadius = instrumentRadius * 1.20f
    val bottomRadius = bodyRadius + ringWidth * 1.72f + lotusRadiusForLayout(ringWidth) + instrumentRadius * 0.2592f
    val canvasCenter = this.center
    withTransform({
        translate(left = center.x - canvasCenter.x, top = center.y - canvasCenter.y)
    }) {
        drawCurvedCenterLabel(
            label = yearLabel,
            radius = topRadius,
            centerAngle = -90f,
            sweep = 74f,
            textSize = ringWidth * 0.58f * 1.5f,
            color = gold.copy(alpha = 0.94f),
            bold = true,
        )
        drawCurvedCenterLabel(
            label = dateLabel,
            radius = bottomRadius,
            centerAngle = 90f,
            sweep = 82f,
            textSize = ringWidth * 0.6f,
            color = Color(0xFFFFE8B0),
            bold = true,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFestivalLotus(
    center: Offset,
    size: Float,
    instrumentRadius: Float,
    lit: Boolean,
    gold: Color,
    label: String?,
) {
    val color = if (lit) gold else Color(0xFF7F6544)
    if (lit) {
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to gold.copy(alpha = 0.34f),
                0.58f to gold.copy(alpha = 0.12f),
                1.0f to Color.Transparent,
                center = center,
                radius = size * 1.05f,
            ),
            radius = size * 1.05f,
            center = center,
        )
    }
    drawSimpleLotus(center, size, color.copy(alpha = if (lit) 0.98f else 0.62f), stroke = size * 0.045f)
    drawCircle(gold.copy(alpha = if (lit) 0.45f else 0.22f), size * 0.54f, center, style = Stroke(width = size * 0.018f))
    if (label != null) {
        drawIntoCanvas { canvas ->
            drawEmbossedText(
                native = canvas.nativeCanvas,
                text = label.uppercase(),
                x = center.x,
                y = center.y + size * 0.5f + instrumentRadius * 0.0648f + size * 0.255f,
                size = size * 0.255f,
                color = Color(0xFFFFE8B0),
                bold = true,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMajorFestivalOverlay(
    festival: FestivalDefinition,
    center: Offset,
    radius: Float,
    gold: Color,
) {
    drawCircle(Color(0xFF050403).copy(alpha = 0.32f), radius * 0.95f, center)
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to gold.copy(alpha = 0.52f),
            0.52f to gold.copy(alpha = 0.2f),
            1.0f to Color.Transparent,
            center = center,
            radius = radius * 1.25f,
        ),
        radius = radius * 1.25f,
        center = center,
    )
    when {
        festival.name.contains("Chaitra New Year") -> drawNewYearIcon(center, radius * 1.08f, gold)
        festival.name.contains("Makara") -> drawMakaraSankrantiIcon(center, radius * 1.12f, gold)
        festival.name.contains("Deepam") -> drawDiyaIcon(center, radius * 1.2f, gold)
        festival.name.contains("Diwali") -> drawDiyaIcon(center, radius * 1.2f, gold)
        festival.name.contains("Holi") -> drawHoliIcon(center, radius * 1.08f, gold)
        festival.name.contains("Janmashtami") -> drawPeacockFeatherIcon(center, radius * 1.14f, gold)
        festival.name.contains("Akshaya") -> drawGoldPotIcon(center, radius * 1.1f, gold)
        festival.name.contains("Shivaratri") -> drawTridentIcon(center, radius * 1.08f, gold)
        festival.name.contains("Ganesh") -> drawGaneshaIcon(center, radius * 1.08f, gold)
        festival.name.contains("Upakarma") -> drawSacredThreadIcon(center, radius * 1.1f, gold)
        festival.name.contains("Thiruvadirai") -> drawNatarajaFlameIcon(center, radius * 1.1f, gold)
        festival.name.contains("Vaikuntha") -> drawVaikunthaGateIcon(center, radius * 1.1f, gold)
        festival.name.contains("Navaratri") || festival.name.contains("Vijayadashami") -> drawTridentIcon(center, radius * 1.06f, gold)
        festival.name.contains("Rama") -> drawBowIcon(center, radius * 1.06f, gold)
        else -> drawSimpleLotus(center, radius * 1.15f, gold, stroke = radius * 0.055f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSimpleLotus(
    center: Offset,
    size: Float,
    color: Color,
    stroke: Float,
) {
    repeat(8) { index ->
        val angle = index * (Math.PI.toFloat() / 4f) - Math.PI.toFloat() / 2f
        val tip = center.polar(angle, size * 0.44f)
        val left = center.polar(angle - 0.42f, size * 0.17f)
        val right = center.polar(angle + 0.42f, size * 0.17f)
        val petal = Path().apply {
            moveTo(center.x, center.y)
            cubicTo(left.x, left.y, left.x, left.y, tip.x, tip.y)
            cubicTo(right.x, right.y, right.x, right.y, center.x, center.y)
            close()
        }
        drawPath(petal, color.copy(alpha = 0.16f))
        drawPath(petal, color, style = Stroke(width = stroke))
    }
    drawCircle(color.copy(alpha = 0.82f), size * 0.08f, center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiyaIcon(center: Offset, size: Float, color: Color) {
    val bowl = Rect(center.x - size * 0.38f, center.y - size * 0.08f, center.x + size * 0.38f, center.y + size * 0.34f)
    drawArc(color.copy(alpha = 0.94f), 0f, 180f, false, bowl.topLeft, Size(bowl.width, bowl.height), style = Stroke(width = size * 0.07f))
    val flame = Path().apply {
        moveTo(center.x, center.y - size * 0.48f)
        cubicTo(center.x - size * 0.22f, center.y - size * 0.14f, center.x - size * 0.08f, center.y + size * 0.02f, center.x, center.y + size * 0.08f)
        cubicTo(center.x + size * 0.18f, center.y - size * 0.08f, center.x + size * 0.2f, center.y - size * 0.28f, center.x, center.y - size * 0.48f)
        close()
    }
    drawPath(flame, color.copy(alpha = 0.3f))
    drawPath(flame, Color(0xFFFFF1BF), style = Stroke(width = size * 0.045f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHoliIcon(center: Offset, size: Float, color: Color) {
    val colors = listOf(color, Color(0xFFE8785F), Color(0xFF74A7FF), Color(0xFF89D485))
    repeat(4) { index ->
        val angle = index * Math.PI.toFloat() / 2f + Math.PI.toFloat() / 4f
        drawCircle(colors[index].copy(alpha = 0.76f), size * 0.18f, center.polar(angle, size * 0.22f))
    }
    drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.9f), size * 0.12f, center, style = Stroke(width = size * 0.045f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNewYearIcon(center: Offset, size: Float, color: Color) {
    drawArc(
        color = color.copy(alpha = 0.92f),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - size * 0.36f, center.y - size * 0.12f),
        size = Size(size * 0.72f, size * 0.72f),
        style = Stroke(width = size * 0.055f, cap = StrokeCap.Round),
    )
    repeat(7) { index ->
        val angle = Math.toRadians((205f + index * 21f).toDouble()).toFloat()
        val inner = center.polar(angle, size * 0.28f)
        val outer = center.polar(angle, size * 0.42f)
        drawLine(color.copy(alpha = 0.76f), inner, outer, strokeWidth = size * 0.028f, cap = StrokeCap.Round)
    }
    val banner = Path().apply {
        moveTo(center.x - size * 0.32f, center.y + size * 0.18f)
        lineTo(center.x + size * 0.32f, center.y + size * 0.18f)
        lineTo(center.x + size * 0.22f, center.y + size * 0.36f)
        lineTo(center.x - size * 0.22f, center.y + size * 0.36f)
        close()
    }
    drawPath(banner, Color(0xFF8E5424).copy(alpha = 0.48f))
    drawPath(banner, color.copy(alpha = 0.92f), style = Stroke(width = size * 0.035f))
    drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.9f), size * 0.09f, center + Offset(0f, size * 0.12f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPeacockFeatherIcon(center: Offset, size: Float, color: Color) {
    val stemStart = center + Offset(-size * 0.22f, size * 0.42f)
    val stemEnd = center + Offset(size * 0.18f, -size * 0.35f)
    drawLine(color.copy(alpha = 0.86f), stemStart, stemEnd, strokeWidth = size * 0.035f, cap = StrokeCap.Round)
    val eyeCenter = center + Offset(size * 0.12f, -size * 0.18f)
    drawOval(
        Color(0xFF2F9B76).copy(alpha = 0.5f),
        topLeft = centeredTopLeft(eyeCenter, size * 0.48f, size * 0.64f),
        size = Size(size * 0.48f, size * 0.64f),
    )
    drawOval(
        color.copy(alpha = 0.94f),
        topLeft = centeredTopLeft(eyeCenter, size * 0.48f, size * 0.64f),
        size = Size(size * 0.48f, size * 0.64f),
        style = Stroke(width = size * 0.035f),
    )
    drawCircle(Color(0xFF1F68B2).copy(alpha = 0.88f), size * 0.15f, eyeCenter)
    drawCircle(Color(0xFF0A163A).copy(alpha = 0.92f), size * 0.075f, eyeCenter)
    repeat(7) { index ->
        val angle = -2.35f + index * 0.26f
        drawLine(
            color.copy(alpha = 0.34f),
            stemEnd,
            stemEnd.polar(angle, size * (0.26f + index * 0.018f)),
            strokeWidth = size * 0.018f,
            cap = StrokeCap.Round,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoldPotIcon(center: Offset, size: Float, color: Color) {
    drawHarvestPot(center, size, color, withSugarcane = false)
    repeat(5) { index ->
        val x = center.x + (index - 2) * size * 0.11f
        val y = center.y - size * (0.22f + if (index % 2 == 0) 0.03f else 0f)
        drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.95f), size * 0.065f, Offset(x, y))
        drawCircle(color.copy(alpha = 0.55f), size * 0.065f, Offset(x, y), style = Stroke(width = size * 0.018f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMakaraSankrantiIcon(center: Offset, size: Float, color: Color) {
    val caneColor = Color(0xFF8BCF72)
    drawSugarcane(center + Offset(-size * 0.23f, -size * 0.02f), size * 0.88f, -18f, caneColor)
    drawSugarcane(center + Offset(size * 0.23f, -size * 0.02f), size * 0.88f, 18f, caneColor)
    drawHarvestPot(center + Offset(0f, size * 0.12f), size * 0.82f, color, withSugarcane = true)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSugarcane(center: Offset, size: Float, rotation: Float, color: Color) {
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        native.save()
        native.rotate(rotation, center.x, center.y)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.copy(alpha = 0.9f).toArgb()
            strokeWidth = size * 0.055f
            strokeCap = android.graphics.Paint.Cap.ROUND
        }
        native.drawLine(center.x, center.y + size * 0.42f, center.x, center.y - size * 0.42f, paint)
        paint.color = Color(0xFFE3F4B3).copy(alpha = 0.8f).toArgb()
        paint.strokeWidth = size * 0.018f
        repeat(5) { index ->
            val y = center.y + size * (0.3f - index * 0.15f)
            native.drawLine(center.x - size * 0.05f, y, center.x + size * 0.05f, y - size * 0.025f, paint)
        }
        native.restore()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHarvestPot(
    center: Offset,
    size: Float,
    color: Color,
    withSugarcane: Boolean,
) {
    val body = Path().apply {
        moveTo(center.x - size * 0.28f, center.y - size * 0.12f)
        cubicTo(center.x - size * 0.44f, center.y + size * 0.08f, center.x - size * 0.3f, center.y + size * 0.34f, center.x, center.y + size * 0.36f)
        cubicTo(center.x + size * 0.3f, center.y + size * 0.34f, center.x + size * 0.44f, center.y + size * 0.08f, center.x + size * 0.28f, center.y - size * 0.12f)
        close()
    }
    drawPath(body, Color(0xFF8E5424).copy(alpha = 0.58f))
    drawPath(body, color.copy(alpha = 0.9f), style = Stroke(width = size * 0.045f))
    drawOval(
        color.copy(alpha = 0.96f),
        topLeft = centeredTopLeft(center, size * 0.58f, size * 0.16f) + Offset(0f, -size * 0.18f),
        size = Size(size * 0.58f, size * 0.16f),
        style = Stroke(width = size * 0.045f),
    )
    if (withSugarcane) {
        drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.84f), size * 0.055f, center + Offset(-size * 0.1f, -size * 0.15f))
        drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.84f), size * 0.055f, center + Offset(size * 0.1f, -size * 0.15f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTridentIcon(center: Offset, size: Float, color: Color) {
    drawLine(color, center + Offset(0f, size * 0.4f), center + Offset(0f, -size * 0.42f), strokeWidth = size * 0.055f, cap = StrokeCap.Round)
    drawLine(color, center + Offset(-size * 0.26f, -size * 0.18f), center + Offset(0f, -size * 0.42f), strokeWidth = size * 0.05f, cap = StrokeCap.Round)
    drawLine(color, center + Offset(size * 0.26f, -size * 0.18f), center + Offset(0f, -size * 0.42f), strokeWidth = size * 0.05f, cap = StrokeCap.Round)
    drawArc(color, 25f, 130f, false, Offset(center.x - size * 0.23f, center.y - size * 0.32f), Size(size * 0.46f, size * 0.42f), style = Stroke(width = size * 0.045f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGaneshaIcon(center: Offset, size: Float, color: Color) {
    drawCircle(color.copy(alpha = 0.15f), size * 0.38f, center)
    drawCircle(color.copy(alpha = 0.88f), size * 0.28f, center + Offset(0f, -size * 0.05f), style = Stroke(width = size * 0.055f))
    drawArc(
        color = color.copy(alpha = 0.94f),
        startAngle = 72f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = Offset(center.x - size * 0.06f, center.y - size * 0.02f),
        size = Size(size * 0.34f, size * 0.48f),
        style = Stroke(width = size * 0.06f, cap = StrokeCap.Round),
    )
    drawCircle(color.copy(alpha = 0.88f), size * 0.11f, center + Offset(-size * 0.32f, -size * 0.09f), style = Stroke(width = size * 0.045f))
    drawCircle(color.copy(alpha = 0.88f), size * 0.11f, center + Offset(size * 0.32f, -size * 0.09f), style = Stroke(width = size * 0.045f))
    drawCircle(Color(0xFF090604).copy(alpha = 0.9f), size * 0.025f, center + Offset(-size * 0.08f, -size * 0.08f))
    drawCircle(Color(0xFF090604).copy(alpha = 0.9f), size * 0.025f, center + Offset(size * 0.08f, -size * 0.08f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSacredThreadIcon(center: Offset, size: Float, color: Color) {
    repeat(3) { index ->
        val inset = index * size * 0.045f
        drawArc(
            color = color.copy(alpha = 0.9f - index * 0.12f),
            startAngle = 128f,
            sweepAngle = 284f,
            useCenter = false,
            topLeft = Offset(center.x - size * 0.34f + inset, center.y - size * 0.4f + inset),
            size = Size(size * 0.68f - inset * 2f, size * 0.8f - inset * 2f),
            style = Stroke(width = size * 0.026f, cap = StrokeCap.Round),
        )
    }
    drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.92f), size * 0.07f, center + Offset(size * 0.16f, size * 0.24f))
    drawLine(
        color.copy(alpha = 0.84f),
        center + Offset(-size * 0.28f, size * 0.3f),
        center + Offset(size * 0.28f, size * 0.3f),
        strokeWidth = size * 0.03f,
        cap = StrokeCap.Round,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNatarajaFlameIcon(center: Offset, size: Float, color: Color) {
    drawCircle(color.copy(alpha = 0.2f), size * 0.4f, center, style = Stroke(width = size * 0.035f))
    repeat(12) { index ->
        val angle = index * Math.PI.toFloat() / 6f
        val inner = center.polar(angle, size * 0.36f)
        val outer = center.polar(angle, size * 0.45f)
        drawLine(color.copy(alpha = 0.7f), inner, outer, strokeWidth = size * 0.026f, cap = StrokeCap.Round)
    }
    drawLine(color, center + Offset(0f, -size * 0.28f), center + Offset(0f, size * 0.26f), strokeWidth = size * 0.05f, cap = StrokeCap.Round)
    drawLine(color, center + Offset(-size * 0.28f, -size * 0.05f), center + Offset(size * 0.28f, -size * 0.05f), strokeWidth = size * 0.045f, cap = StrokeCap.Round)
    drawLine(color, center + Offset(-size * 0.08f, size * 0.16f), center + Offset(size * 0.24f, size * 0.3f), strokeWidth = size * 0.045f, cap = StrokeCap.Round)
    drawArc(
        color = Color(0xFFFFF1BF).copy(alpha = 0.82f),
        startAngle = 210f,
        sweepAngle = 230f,
        useCenter = false,
        topLeft = Offset(center.x - size * 0.22f, center.y - size * 0.32f),
        size = Size(size * 0.44f, size * 0.44f),
        style = Stroke(width = size * 0.035f, cap = StrokeCap.Round),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVaikunthaGateIcon(center: Offset, size: Float, color: Color) {
    val rect = Rect(center.x - size * 0.32f, center.y - size * 0.28f, center.x + size * 0.32f, center.y + size * 0.38f)
    drawArc(
        color = color.copy(alpha = 0.94f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.width),
        style = Stroke(width = size * 0.055f, cap = StrokeCap.Round),
    )
    drawLine(color.copy(alpha = 0.94f), Offset(rect.left, rect.top + rect.width * 0.5f), Offset(rect.left, rect.bottom), strokeWidth = size * 0.055f, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = 0.94f), Offset(rect.right, rect.top + rect.width * 0.5f), Offset(rect.right, rect.bottom), strokeWidth = size * 0.055f, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = 0.72f), Offset(rect.left - size * 0.08f, rect.bottom), Offset(rect.right + size * 0.08f, rect.bottom), strokeWidth = size * 0.055f, cap = StrokeCap.Round)
    drawLine(Color(0xFFFFF1BF).copy(alpha = 0.72f), center + Offset(0f, -size * 0.08f), center + Offset(0f, size * 0.26f), strokeWidth = size * 0.035f, cap = StrokeCap.Round)
    drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.9f), size * 0.04f, center + Offset(0f, size * 0.02f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBowIcon(center: Offset, size: Float, color: Color) {
    drawArc(
        color = color,
        startAngle = -72f,
        sweepAngle = 144f,
        useCenter = false,
        topLeft = Offset(center.x - size * 0.48f, center.y - size * 0.48f),
        size = Size(size * 0.96f, size * 0.96f),
        style = Stroke(width = size * 0.055f, cap = StrokeCap.Round),
    )
    drawLine(color.copy(alpha = 0.88f), center + Offset(-size * 0.18f, -size * 0.42f), center + Offset(-size * 0.18f, size * 0.42f), strokeWidth = size * 0.03f)
    drawLine(color, center + Offset(-size * 0.18f, 0f), center + Offset(size * 0.36f, 0f), strokeWidth = size * 0.055f, cap = StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCryptexPicker(
    layout: CryptexLayout,
    year: Int,
    month: Int,
    day: Int,
) {
    drawRect(Color.Black.copy(alpha = 0.58f))
    val rect = layout.outerRect
    val corner = CornerRadius(rect.height * 0.08f, rect.height * 0.08f)
    drawRoundRect(
        brush = Brush.linearGradient(
            0.0f to Color(0xFF1C0D06),
            0.28f to Color(0xFF71451F),
            0.52f to Color(0xFFD4944D),
            0.78f to Color(0xFF4B2711),
            1.0f to Color(0xFF180B05),
            start = Offset(rect.left, rect.top),
            end = Offset(rect.right, rect.bottom),
        ),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        cornerRadius = corner,
    )
    drawRoundRect(
        color = Color(0xFFFFE5A3).copy(alpha = 0.84f),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        cornerRadius = corner,
        style = Stroke(width = 1.5.dp.toPx()),
    )

    val values = listOf(
        RollerValues((year - 1).toString(), year.toString(), (year + 1).toString()),
        RollerValues(monthName(month - 1), monthName(month), monthName(month + 1)),
        RollerValues(previousDay(day, year, month).toString().padStart(2, '0'), day.toString().padStart(2, '0'), nextDay(day, year, month).toString().padStart(2, '0')),
    )
    layout.columns.forEachIndexed { index, column ->
        drawCryptexColumn(column, values[index])
    }
    drawRoundRect(
        color = Color(0xFF0A0503).copy(alpha = 0.62f),
        topLeft = layout.commitRect.topLeft,
        size = Size(layout.commitRect.width, layout.commitRect.height),
        cornerRadius = CornerRadius(layout.commitRect.height * 0.45f, layout.commitRect.height * 0.45f),
    )
    drawRoundRect(
        color = Color(0xFFFFE5A3).copy(alpha = 0.58f),
        topLeft = layout.commitRect.topLeft,
        size = Size(layout.commitRect.width, layout.commitRect.height),
        cornerRadius = CornerRadius(layout.commitRect.height * 0.45f, layout.commitRect.height * 0.45f),
        style = Stroke(width = 0.9.dp.toPx()),
    )
    drawIntoCanvas { canvas ->
        drawEmbossedText(
            native = canvas.nativeCanvas,
            text = "SET DATE",
            x = layout.commitRect.center.x,
            y = layout.commitRect.center.y + layout.commitRect.height * 0.18f,
            size = layout.commitRect.height * 0.48f,
            color = Color(0xFFFFE3A5),
            bold = true,
        )
    }
}

private data class RollerValues(
    val previous: String,
    val current: String,
    val next: String,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCryptexColumn(
    rect: Rect,
    values: RollerValues,
) {
    val corner = CornerRadius(rect.width * 0.22f, rect.width * 0.22f)
    drawRoundRect(
        brush = Brush.linearGradient(
            0.0f to Color(0xFF160A04),
            0.22f to Color(0xFFB8783B),
            0.5f to Color(0xFFE7B66D),
            0.78f to Color(0xFF7E451D),
            1.0f to Color(0xFF130804),
            start = Offset(rect.left, rect.top),
            end = Offset(rect.right, rect.top),
        ),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        cornerRadius = corner,
    )
    drawRoundRect(
        color = Color(0xFF0A0503).copy(alpha = 0.42f),
        topLeft = Offset(rect.left, rect.top + rect.height * 0.34f),
        size = Size(rect.width, rect.height * 0.32f),
        cornerRadius = CornerRadius(rect.width * 0.14f, rect.width * 0.14f),
    )
    drawRoundRect(
        color = Color(0xFFFFE5A3).copy(alpha = 0.6f),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        cornerRadius = corner,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val x = rect.center.x
        drawEmbossedText(native, values.previous, x, rect.top + rect.height * 0.24f, rect.height * 0.14f, Color(0xFFFFE3A5).copy(alpha = 0.56f), false)
        drawEmbossedText(native, values.current, x, rect.center.y + rect.height * 0.07f, rect.height * 0.23f, Color(0xFFFFF0BD), true)
        drawEmbossedText(native, values.next, x, rect.bottom - rect.height * 0.16f, rect.height * 0.14f, Color(0xFFFFE3A5).copy(alpha = 0.56f), false)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDeviceLighting(
    center: Offset,
    radius: Float,
    solarAltitude: Float,
    moonlight: Float,
    gold: Color,
) {
    val daylight = ((solarAltitude + 8f) / 58f).coerceIn(0f, 1f)
    val noon = ((solarAltitude - 28f) / 40f).coerceIn(0f, 1f)
    val twilight = (1f - (abs(solarAltitude - 1f) / 15f)).coerceIn(0f, 1f)
    val night = (1f - ((solarAltitude + 8f) / 12f)).coerceIn(0f, 1f)
    val nightBlue = lerpColor(Color(0xFF020715), Color(0xFF102D5A), moonlight.coerceIn(0f, 1f))
    val dayCore = lerpColor(Color(0xFF4A3517), Color(0xFFFFEDBA), noon)
    val core = lerpColor(nightBlue, dayCore, daylight)
    val rim = lerpColor(Color(0xFF030511), Color(0xFF7C5425), (daylight + twilight * 0.8f).coerceIn(0f, 1f))

    drawCircle(
        brush = Brush.radialGradient(
            0.0f to core.copy(alpha = 0.74f),
            0.56f to lerpColor(Color(0xFF091125), Color(0xFF3A2410), daylight).copy(alpha = 0.56f),
            1.0f to rim.copy(alpha = 0.82f),
            center = center + Offset(0f, -radius * 0.18f),
            radius = radius * 1.08f,
        ),
        radius = radius,
        center = center,
    )
    if (twilight > 0f) {
        drawCircle(
            brush = Brush.linearGradient(
                0.0f to Color.Transparent,
                0.45f to gold.copy(alpha = 0.08f + twilight * 0.32f),
                1.0f to Color.Transparent,
                start = center + Offset(-radius * 0.95f, radius * 0.42f),
                end = center + Offset(radius * 0.95f, -radius * 0.42f),
            ),
            radius = radius,
            center = center,
        )
    }
    if (night > 0f) {
        drawStars(center, radius, night)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStars(
    center: Offset,
    radius: Float,
    alpha: Float,
) {
    repeat(42) { index ->
        val a = Math.toRadians(((index * 137 + 29) % 360).toDouble()).toFloat()
        val r = radius * (0.18f + ((index * 41) % 76) / 100f)
        val position = center.polar(a, r)
        val size = radius * (0.0038f + ((index % 5) * 0.0011f))
        drawCircle(Color(0xFFE8F1FF).copy(alpha = alpha * (0.18f + (index % 4) * 0.1f)), size, position)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMetalCircleBoundaries(
    center: Offset,
    radii: List<Float>,
    width: Float,
    color: Color,
) {
    radii.forEach { radius ->
        val outer = radius + width * 0.42f
        val inner = radius - width * 0.42f
        drawCircle(color.copy(alpha = 0.58f), outer, center, style = Stroke(width = 0.75.dp.toPx()))
        drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.26f), outer - width * 0.035f, center, style = Stroke(width = 0.55.dp.toPx()))
        drawCircle(color.copy(alpha = 0.38f), inner, center, style = Stroke(width = 0.65.dp.toPx()))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlassLayer(
    center: Offset,
    radius: Float,
    innerRadius: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.64f to Color.Transparent,
            1.0f to Color(0xFFB9D8FF).copy(alpha = 0.11f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(Color(0xFFBFDFFF).copy(alpha = 0.08f), innerRadius, center, style = Stroke(width = 0.7.dp.toPx()))
    drawArc(
        color = Color.White.copy(alpha = 0.18f),
        startAngle = 204f,
        sweepAngle = 78f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.73f, center.y - radius * 0.73f),
        size = Size(radius * 1.46f, radius * 1.46f),
        style = Stroke(width = radius * 0.045f, cap = StrokeCap.Round),
    )
    drawArc(
        color = Color(0xFFBFDFFF).copy(alpha = 0.16f),
        startAngle = 18f,
        sweepAngle = 88f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.86f, center.y - radius * 0.86f),
        size = Size(radius * 1.72f, radius * 1.72f),
        style = Stroke(width = radius * 0.018f, cap = StrokeCap.Round),
    )
    drawCircle(Color.White.copy(alpha = 0.2f), radius, center, style = Stroke(width = 0.8.dp.toPx()))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRing(
    segmentCount: Int,
    activeIndex: Int,
    radius: Float,
    width: Float,
    inactive: Color,
    active: Color,
) {
    val sweep = 360f / segmentCount
    val rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
    for (index in 0 until segmentCount) {
        val color = if (index == activeIndex) active else inactive.copy(alpha = 0.22f)
        drawArc(
            color = color,
            startAngle = -90f + index * sweep + 1.2f,
            sweepAngle = sweep - 2.4f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = Size(rect.width, rect.height),
            style = Stroke(width = width * 0.78f, cap = StrokeCap.Butt),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLagnaRashiRing(
    sectors: List<LagnaSector>,
    activeLagnaIndex: Int?,
    dayFraction: Double,
    radius: Float,
    width: Float,
    inactive: Color,
    activeLagna: Color,
) {
    val outerRadius = radius + width * 0.42f
    val innerRadius = radius - width * 0.42f
    val outerRect = Rect(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius)
    val innerRect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius)
    val lineWidth = 0.9.dp.toPx()
    sectors.forEach { sector ->
        val startAngle = -90f + (sector.startFraction * 360.0).toFloat()
        val sweep = (sector.durationFraction * 360.0).toFloat()
        val lagnaActive = activeLagnaIndex != null && sector.index == activeLagnaIndex && dayFraction >= sector.startFraction && dayFraction <= sector.startFraction + sector.durationFraction
        val line = when {
            lagnaActive -> activeLagna.copy(alpha = 0.96f)
            else -> inactive.copy(alpha = 0.82f)
        }
        val gap = min(1.2f, sweep * 0.12f)
        drawArc(
            color = line,
            startAngle = startAngle + gap / 2f,
            sweepAngle = (sweep - gap).coerceAtLeast(0.2f),
            useCenter = false,
            topLeft = outerRect.topLeft,
            size = Size(outerRect.width, outerRect.height),
            style = Stroke(width = lineWidth, cap = StrokeCap.Butt),
        )
        drawArc(
            color = line.copy(alpha = if (lagnaActive) 0.72f else 0.44f),
            startAngle = startAngle + gap / 2f,
            sweepAngle = (sweep - gap).coerceAtLeast(0.2f),
            useCenter = false,
            topLeft = innerRect.topLeft,
            size = Size(innerRect.width, innerRect.height),
            style = Stroke(width = lineWidth, cap = StrokeCap.Butt),
        )
        val startRad = Math.toRadians(startAngle.toDouble()).toFloat()
        val endRad = Math.toRadians((startAngle + sweep).toDouble()).toFloat()
        drawLine(line.copy(alpha = 0.62f), center.polar(startRad, innerRadius), center.polar(startRad, outerRadius), lineWidth)
        drawLine(line.copy(alpha = 0.62f), center.polar(endRad, innerRadius), center.polar(endRad, outerRadius), lineWidth)
    }
    drawLagnaHand(sectors, dayFraction, radius, width, activeLagna)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLagnaHand(
    sectors: List<LagnaSector>,
    dayFraction: Double,
    radius: Float,
    width: Float,
    color: Color,
) {
    val angleDegrees = lagnaHandAngle(sectors, dayFraction)
    val angle = Math.toRadians(angleDegrees.toDouble()).toFloat()
    val start = center.polar(angle + Math.PI.toFloat(), width * 0.35f)
    val tip = center.polar(angle, radius - width * 0.54f)
    val neck = center.polar(angle, radius - width * 0.82f)
    val left = angle + Math.PI.toFloat() / 2f
    val right = angle - Math.PI.toFloat() / 2f
    val leafLeft = neck.polar(left, width * 0.18f)
    val leafRight = neck.polar(right, width * 0.18f)
    val curlLeft = center.polar(angle, radius - width * 1.02f).polar(left, width * 0.11f)
    val curlRight = center.polar(angle, radius - width * 1.02f).polar(right, width * 0.11f)
    drawLine(Color(0xFF0B0906).copy(alpha = 0.78f), start, neck, strokeWidth = width * 0.16f, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = 0.96f), start, neck, strokeWidth = width * 0.075f, cap = StrokeCap.Round)
    val shadowHead = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(leafLeft.x, leafLeft.y, curlLeft.x, curlLeft.y, neck.x, neck.y)
        cubicTo(curlRight.x, curlRight.y, leafRight.x, leafRight.y, tip.x, tip.y)
        close()
    }
    drawPath(shadowHead, Color(0xFF120D08).copy(alpha = 0.86f))
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(leafLeft.x, leafLeft.y, curlLeft.x, curlLeft.y, neck.x, neck.y)
        cubicTo(curlRight.x, curlRight.y, leafRight.x, leafRight.y, tip.x, tip.y)
        close()
    }
    drawPath(head, color.copy(alpha = 0.96f))
    drawPath(head, Color(0xFFFFF1BD).copy(alpha = 0.34f), style = Stroke(width = width * 0.018f))
}

private fun lagnaHandAngle(sectors: List<LagnaSector>, dayFraction: Double): Float {
    val fraction = dayFraction.coerceIn(0.0, 1.0)
    val sector = sectors.firstOrNull { fraction >= it.startFraction && fraction <= it.startFraction + it.durationFraction }
    val ringFraction = if (sector == null || sector.durationFraction <= 0.0) {
        fraction
    } else {
        val progress = ((fraction - sector.startFraction) / sector.durationFraction).coerceIn(0.0, 1.0)
        sector.startFraction + sector.durationFraction * progress
    }
    return -90f + (ringFraction * 360.0).toFloat()
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMonthRing(
    sectors: List<MonthSector>,
    activeIndex: Int,
    radius: Float,
    width: Float,
    inactive: Color,
    active: Color,
) {
    val outerRadius = radius + width * 0.42f
    val innerRadius = radius - width * 0.42f
    val outerRect = Rect(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius)
    val innerRect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius)
    val lineWidth = 0.9.dp.toPx()
    var start = -90f
    sectors.forEach { sector ->
        val sweep = sector.arcDegrees.toFloat()
        val activeCell = sector.index == activeIndex
        val line = if (activeCell) active.copy(alpha = 0.9f) else inactive.copy(alpha = 0.82f)
        val text = if (activeCell) active.copy(alpha = 0.98f) else Color(0xFFB98C56).copy(alpha = 0.62f)
        val gap = min(1.2f, sweep * 0.12f)

        drawArc(
            color = line,
            startAngle = start + gap / 2f,
            sweepAngle = (sweep - gap).coerceAtLeast(0.2f),
            useCenter = false,
            topLeft = outerRect.topLeft,
            size = Size(outerRect.width, outerRect.height),
            style = Stroke(width = lineWidth, cap = StrokeCap.Butt),
        )
        drawArc(
            color = line.copy(alpha = if (activeCell) 0.72f else 0.44f),
            startAngle = start + gap / 2f,
            sweepAngle = (sweep - gap).coerceAtLeast(0.2f),
            useCenter = false,
            topLeft = innerRect.topLeft,
            size = Size(innerRect.width, innerRect.height),
            style = Stroke(width = lineWidth, cap = StrokeCap.Butt),
        )
        val startRad = Math.toRadians(start.toDouble()).toFloat()
        val endRad = Math.toRadians((start + sweep).toDouble()).toFloat()
        drawLine(line.copy(alpha = 0.62f), center.polar(startRad, innerRadius), center.polar(startRad, outerRadius), lineWidth)
        drawLine(line.copy(alpha = 0.62f), center.polar(endRad, innerRadius), center.polar(endRad, outerRadius), lineWidth)
        drawSectorLabel(sector.abbreviation, start + sweep / 2f, radius, width, text, activeCell, 12)
        start += sweep
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTithiRing(
    activeIndex: Int,
    radius: Float,
    width: Float,
    inactive: Color,
    active: Color,
) {
    drawTithiCells(activeIndex, radius, width, inactive, active)
    drawPakshaGuide(activeIndex, radius, width, active, Color(0xFF9AA0A6))
    drawTithiHand(activeIndex, radius, width, active)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTithiCells(
    activeIndex: Int,
    radius: Float,
    width: Float,
    inactive: Color,
    active: Color,
) {
    val outerRadius = radius + width * 0.42f
    val innerRadius = radius - width * 0.42f
    val outerRect = Rect(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius)
    val innerRect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius)
    val sweep = 12f
    val lineWidth = 0.9.dp.toPx()

    for (index in 0 until 30) {
        val start = tithiCellStartAngle(index)
        val activeCell = index == activeIndex
        val line = if (activeCell) active.copy(alpha = 0.9f) else inactive.copy(alpha = 0.82f)
        val text = if (activeCell) active.copy(alpha = 0.98f) else Color(0xFFB98C56).copy(alpha = 0.62f)

        drawArc(
            color = line,
            startAngle = start + 0.6f,
            sweepAngle = sweep - 1.2f,
            useCenter = false,
            topLeft = outerRect.topLeft,
            size = Size(outerRect.width, outerRect.height),
            style = Stroke(width = lineWidth, cap = StrokeCap.Butt),
        )
        drawArc(
            color = line.copy(alpha = if (activeCell) 0.72f else 0.44f),
            startAngle = start + 0.6f,
            sweepAngle = sweep - 1.2f,
            useCenter = false,
            topLeft = innerRect.topLeft,
            size = Size(innerRect.width, innerRect.height),
            style = Stroke(width = lineWidth, cap = StrokeCap.Butt),
        )

        val startRad = Math.toRadians(start.toDouble()).toFloat()
        val endRad = Math.toRadians((start + sweep).toDouble()).toFloat()
        drawLine(line.copy(alpha = 0.62f), center.polar(startRad, innerRadius), center.polar(startRad, outerRadius), lineWidth)
        drawLine(line.copy(alpha = 0.62f), center.polar(endRad, innerRadius), center.polar(endRad, outerRadius), lineWidth)
        if (index % 15 == 14) {
            val anchorAngle = if (index == 14) -90f else 90f
            drawTithiFifteenthMarker(anchorAngle, radius - width * 0.3f, width, text, filled = index == 14)
        } else {
            drawSectorLabel(((index % 15) + 1).toString(), start + sweep / 2f, radius, width, text, activeCell, 30)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTithiFifteenthMarker(
    angleDegrees: Float,
    radius: Float,
    width: Float,
    color: Color,
    filled: Boolean,
) {
    val angle = Math.toRadians(angleDegrees.toDouble()).toFloat()
    val position = center.polar(angle, radius)
    if (filled) {
        drawCircle(color.copy(alpha = 0.98f), width * 0.36f, position)
        drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.45f), width * 0.16f, position + Offset(-width * 0.05f, -width * 0.05f))
    } else {
        drawCircle(color.copy(alpha = 0.96f), width * 0.36f, position)
        drawCircle(Color(0xFF0B0906).copy(alpha = 0.94f), width * 0.29f, position)
        drawCircle(Color(0xFFFFF1BF).copy(alpha = 0.5f), width * 0.36f, position, style = Stroke(width = width * 0.022f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTithiHand(
    activeIndex: Int,
    radius: Float,
    width: Float,
    color: Color,
) {
    val markerAngle = tithiCellCenterAngle(activeIndex)
    val angle = Math.toRadians(markerAngle.toDouble()).toFloat()
    val handStart = center.polar(angle + Math.PI.toFloat(), width * 0.55f)
    val tip = center.polar(angle, radius - width * 0.54f)
    val neck = center.polar(angle, radius - width * 0.82f)
    val left = angle + Math.PI.toFloat() / 2f
    val right = angle - Math.PI.toFloat() / 2f
    val leafLeft = neck.polar(left, width * 0.18f)
    val leafRight = neck.polar(right, width * 0.18f)
    val curlLeft = center.polar(angle, radius - width * 1.02f).polar(left, width * 0.11f)
    val curlRight = center.polar(angle, radius - width * 1.02f).polar(right, width * 0.11f)

    drawLine(
        color = Color(0xFF120D08).copy(alpha = 0.78f),
        start = handStart,
        end = neck,
        strokeWidth = width * 0.16f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color.copy(alpha = 0.98f),
        start = handStart,
        end = neck,
        strokeWidth = width * 0.075f,
        cap = StrokeCap.Round,
    )
    val shadowHead = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(leafLeft.x, leafLeft.y, curlLeft.x, curlLeft.y, neck.x, neck.y)
        cubicTo(curlRight.x, curlRight.y, leafRight.x, leafRight.y, tip.x, tip.y)
        close()
    }
    drawPath(shadowHead, Color(0xFF120D08).copy(alpha = 0.86f))

    val head = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(leafLeft.x, leafLeft.y, curlLeft.x, curlLeft.y, neck.x, neck.y)
        cubicTo(curlRight.x, curlRight.y, leafRight.x, leafRight.y, tip.x, tip.y)
        close()
    }
    drawPath(head, color.copy(alpha = 0.96f))
    drawPath(head, Color(0xFFFFF1BD).copy(alpha = 0.34f), style = Stroke(width = width * 0.018f))

    val counter = center.polar(angle + Math.PI.toFloat(), width * 0.82f)
    drawCircle(Color(0xFF120D08).copy(alpha = 0.78f), width * 0.16f, counter)
    drawCircle(color.copy(alpha = 0.95f), width * 0.09f, counter)
    drawCircle(Color(0xFF120D08).copy(alpha = 0.9f), width * 0.045f, counter)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPakshaGuide(
    activeIndex: Int,
    radius: Float,
    width: Float,
    shukla: Color,
    krishna: Color,
) {
    val guideRadius = radius - width * 0.3f
    val rect = Rect(center.x - guideRadius, center.y - guideRadius, center.x + guideRadius, center.y + guideRadius)
    val guideWidth = width * 0.12f

    drawArc(
        color = shukla.copy(alpha = 0.9f),
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        style = Stroke(width = guideWidth, cap = StrokeCap.Round),
    )
    drawArc(
        color = krishna.copy(alpha = 0.62f),
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        style = Stroke(width = guideWidth, cap = StrokeCap.Round),
    )

    drawAmavasyaAnchor(90f, guideRadius, width, Color(0xFFE8CA8B))
    drawPournamiAnchor(-90f, guideRadius, width, Color(0xFFE8CA8B))

    val markerAngle = tithiCellCenterAngle(activeIndex)
    val markerColor = if (activeIndex < 15) shukla else krishna
    val markerPosition = center.polar(Math.toRadians(markerAngle.toDouble()).toFloat(), guideRadius)
    drawCircle(Color(0xFF0B0906).copy(alpha = 0.85f), width * 0.17f, markerPosition)
    drawCircle(markerColor.copy(alpha = 0.98f), width * 0.1f, markerPosition)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmavasyaAnchor(
    angleDegrees: Float,
    radius: Float,
    width: Float,
    color: Color,
) {
    val position = center.polar(Math.toRadians(angleDegrees.toDouble()).toFloat(), radius)
    drawCircle(Color(0xFF120D08).copy(alpha = 0.96f), width * 0.2f, position)
    drawCircle(color.copy(alpha = 0.95f), width * 0.18f, position, style = Stroke(width = width * 0.04f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPournamiAnchor(
    angleDegrees: Float,
    radius: Float,
    width: Float,
    color: Color,
) {
    val position = center.polar(Math.toRadians(angleDegrees.toDouble()).toFloat(), radius)
    drawCircle(Color(0xFF120D08).copy(alpha = 0.82f), width * 0.22f, position)
    drawCircle(color.copy(alpha = 0.98f), width * 0.17f, position)
    drawCircle(Color(0xFFFFF4CC).copy(alpha = 0.38f), width * 0.1f, position + Offset(-width * 0.035f, -width * 0.035f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSectorCells(
    segmentCount: Int,
    activeIndex: Int,
    radius: Float,
    width: Float,
    inactive: Color,
    active: Color,
    labels: List<String>,
) {
    val outerRadius = radius + width * 0.42f
    val innerRadius = radius - width * 0.42f
    val outerRect = Rect(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius)
    val innerRect = Rect(center.x - innerRadius, center.y - innerRadius, center.x + innerRadius, center.y + innerRadius)
    val sweep = 360f / segmentCount
    val lineWidth = 0.9.dp.toPx()
    var start = -90f
    for (index in 0 until segmentCount) {
        val activeCell = index == activeIndex
        val line = if (activeCell) active.copy(alpha = 0.86f) else inactive.copy(alpha = 0.82f)
        val text = if (activeCell) active.copy(alpha = 0.98f) else Color(0xFFB98C56).copy(alpha = 0.62f)
        drawArc(
            color = line,
            startAngle = start + 0.6f,
            sweepAngle = sweep - 1.2f,
            useCenter = false,
            topLeft = outerRect.topLeft,
            size = Size(outerRect.width, outerRect.height),
            style = Stroke(width = lineWidth, cap = StrokeCap.Butt),
        )
        drawArc(
            color = line.copy(alpha = if (activeCell) 0.72f else 0.44f),
            startAngle = start + 0.6f,
            sweepAngle = sweep - 1.2f,
            useCenter = false,
            topLeft = innerRect.topLeft,
            size = Size(innerRect.width, innerRect.height),
            style = Stroke(width = lineWidth, cap = StrokeCap.Butt),
        )
        val startRad = Math.toRadians(start.toDouble()).toFloat()
        val endRad = Math.toRadians((start + sweep).toDouble()).toFloat()
        drawLine(line.copy(alpha = 0.62f), center.polar(startRad, innerRadius), center.polar(startRad, outerRadius), lineWidth)
        drawLine(line.copy(alpha = 0.62f), center.polar(endRad, innerRadius), center.polar(endRad, outerRadius), lineWidth)
        drawSectorLabel(labels.getOrElse(index) { "" }, start + sweep / 2f, radius, width, text, activeCell, segmentCount)
        start += sweep
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSectorLabel(
    label: String,
    angleDegrees: Float,
    radius: Float,
    width: Float,
    color: Color,
    active: Boolean,
    segmentCount: Int,
) {
    if (label.isBlank()) return
    val angle = Math.toRadians(angleDegrees.toDouble()).toFloat()
    val position = center.polar(angle, radius)
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = width * if (segmentCount <= 12) 0.64f else 0.46f
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SERIF,
                if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL,
            )
        }
        native.save()
        native.rotate(angleDegrees + 90f, position.x, position.y)
        native.drawText(label, position.x, position.y + paint.textSize * 0.34f, paint)
        native.restore()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRashiSigilRing(
    activeIndex: Int,
    lagnaIndex: Int?,
    sectors: List<LagnaSector>,
    radius: Float,
    iconSize: Float,
    trackWidth: Float,
    inactive: Color,
    active: Color,
) {
    val positions = if (sectors.isEmpty()) {
        CalendarCatalog.rashis.mapIndexed { index, _ -> Triple(index, (index + 0.5) / 12.0, 1.0) }
    } else {
        sectors.map { sector -> Triple(sector.index, sector.startFraction + sector.durationFraction * 0.5, sector.durationFraction * 12.0) }
    }
    positions.forEach { (index, fraction, widthRatio) ->
        val angle = Math.toRadians((-90f + fraction * 360.0).toDouble())
        val position = Offset(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius,
        )
        val scale = widthRatio.toFloat().coerceIn(0.72f, 1.24f)
        val scaledIconSize = iconSize * scale
        val maxSigilSize = trackWidth * 0.92f
        val baseSigilSize = (scaledIconSize * 0.9f).coerceAtMost(maxSigilSize)
        if (index == lagnaIndex) {
            drawCircle(Color(0xFF0B0906).copy(alpha = 0.78f), baseSigilSize * 0.55f, position)
            drawCircle(Color(0xFFFFE2A3).copy(alpha = 0.42f), baseSigilSize * 0.58f, position, style = Stroke(width = baseSigilSize * 0.06f))
        }
        if (index == activeIndex) {
            val sigilSize = (scaledIconSize * 1.28f).coerceAtMost(maxSigilSize)
            drawCircle(Color(0xFF0B0906).copy(alpha = 0.82f), sigilSize * 0.46f, position)
            drawCircle(active.copy(alpha = 0.28f), sigilSize * 0.52f, position, style = Stroke(width = sigilSize * 0.055f))
            drawRashiSigil(index, position, sigilSize, active)
        } else {
            drawRashiSigil(index, position, baseSigilSize, if (index == lagnaIndex) Color(0xFFFFE2A3) else inactive)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNakshatraSigilRing(
    activeIndex: Int,
    radius: Float,
    iconSize: Float,
    inactive: Color,
    active: Color,
    images: SigilImages,
) {
    val sweep = 360f / 27f
    for (index in 0 until 27) {
        val angle = Math.toRadians((-90f + (index + 0.5f) * sweep).toDouble())
        val position = Offset(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius,
        )
        if (index == activeIndex) {
            drawCircle(Color(0xFF0B0906).copy(alpha = 0.84f), iconSize * 0.82f, position)
            drawCircle(active.copy(alpha = 0.26f), iconSize * 0.94f, position, style = Stroke(width = iconSize * 0.08f))
            drawNakshatraSigil(index, position, iconSize * 1.61f, active, images)
        } else {
            drawNakshatraSigil(index, position, iconSize * 0.82f, inactive, images)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoon(
    center: Offset,
    radius: Float,
    illumination: Double,
    lunarLongitude: Double,
    solarLongitude: Double,
) {
    val phase = normalizePhase(lunarLongitude - solarLongitude)
    val waxing = phase <= 180.0
    val bright = Color(0xFFE7E0CE)
    val mid = Color(0xFF8D887D)
    val shadow = Color(0xFF15130F)

    drawCircle(shadow, radius, center)
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val clip = android.graphics.Path().apply {
            addCircle(center.x, center.y, radius, android.graphics.Path.Direction.CW)
        }
        native.save()
        native.clipPath(clip)

        val lightPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                center.x - radius * 0.2f,
                center.y - radius * 0.24f,
                radius * 1.25f,
                intArrayOf(bright.toArgb(), mid.toArgb(), Color(0xFF4B4942).toArgb()),
                floatArrayOf(0f, 0.62f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
            style = android.graphics.Paint.Style.FILL
        }
        val phaseCos = cos(Math.toRadians(phase)).toFloat().coerceIn(-1f, 1f)
        val path = android.graphics.Path()
        val steps = 48
        if (waxing) {
            path.moveTo(center.x - radius, center.y)
            for (step in 0..steps) {
                val x = -radius + (2f * radius * step / steps)
                val edge = sqrt((radius * radius - x * x).coerceAtLeast(0f))
                path.lineTo(center.x + x, center.y + edge)
            }
            for (step in steps downTo 0) {
                val x = -radius + (2f * radius * step / steps)
                val edge = sqrt((radius * radius - x * x).coerceAtLeast(0f))
                path.lineTo(center.x + x, center.y + phaseCos * edge)
            }
        } else {
            path.moveTo(center.x - radius, center.y)
            for (step in 0..steps) {
                val x = -radius + (2f * radius * step / steps)
                val edge = sqrt((radius * radius - x * x).coerceAtLeast(0f))
                path.lineTo(center.x + x, center.y - edge)
            }
            for (step in steps downTo 0) {
                val x = -radius + (2f * radius * step / steps)
                val edge = sqrt((radius * radius - x * x).coerceAtLeast(0f))
                path.lineTo(center.x + x, center.y - phaseCos * edge)
            }
        }
        path.close()
        native.drawPath(path, lightPaint)
        native.restore()
    }
    drawMoonCraters(center, radius, illumination.toFloat().coerceIn(0f, 1f))
    drawCircle(Color(0xFF050403).copy(alpha = 0.38f), radius, center, style = Stroke(width = radius * 0.065f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoonCraters(center: Offset, radius: Float, illumination: Float) {
    val craters = listOf(
        Offset(-0.28f, -0.23f) to 0.11f,
        Offset(0.18f, -0.31f) to 0.075f,
        Offset(0.29f, 0.08f) to 0.13f,
        Offset(-0.18f, 0.22f) to 0.085f,
        Offset(0.02f, 0.32f) to 0.052f,
        Offset(-0.02f, -0.02f) to 0.065f,
    )
    craters.forEach { (p, r) ->
        val craterCenter = center + Offset(p.x * radius, p.y * radius)
        drawCircle(Color(0xFF4B4942).copy(alpha = 0.1f + illumination * 0.22f), radius * r, craterCenter)
        drawCircle(Color(0xFFF4EEDB).copy(alpha = 0.08f + illumination * 0.14f), radius * r, craterCenter, style = Stroke(width = radius * 0.015f))
    }
}

private fun normalizePhase(value: Double): Double {
    val normalized = value % 360.0
    return if (normalized < 0) normalized + 360.0 else normalized
}

private fun tithiCellStartAngle(index: Int): Float =
    if (index < 15) {
        96f + index * 12f
    } else {
        -84f + (index - 15) * 12f
    }

private fun tithiCellCenterAngle(index: Int): Float = tithiCellStartAngle(index) + 6f

private fun Offset.isInRect(rect: Rect): Boolean =
    x in rect.left..rect.right && y in rect.top..rect.bottom

private fun centeredTopLeft(center: Offset, width: Float, height: Float): Offset =
    Offset(center.x - width / 2f, center.y - height / 2f)

private fun Offset.polar(angle: Float, radius: Float): Offset =
    this + Offset(cos(angle) * radius, sin(angle) * radius)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurvedActiveReadout(
    rashi: String,
    nakshatra: String,
    radius: Float,
    textSize: Float,
    nakshatraRadiusOffset: Float,
    nakshatraTextScale: Float,
    text: Color,
    accent: Color,
) {
    drawCurvedCenterLabel(
        label = rashi.uppercase(),
        radius = radius,
        centerAngle = -90f,
        sweep = 108f,
        textSize = textSize,
        color = accent,
        bold = true,
    )
    drawCurvedCenterLabel(
        label = nakshatra.uppercase(),
        radius = radius + nakshatraRadiusOffset,
        centerAngle = 90f,
        sweep = 108f,
        textSize = textSize * nakshatraTextScale,
        color = text.copy(alpha = 0.86f),
        bold = true,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurvedCenterLabel(
    label: String,
    radius: Float,
    centerAngle: Float,
    sweep: Float,
    textSize: Float,
    color: Color,
    bold: Boolean,
) {
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textAlign = android.graphics.Paint.Align.LEFT
            this.textSize = textSize
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SERIF,
                if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL,
            )
        }
        val rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        val path = android.graphics.Path()
        val pathRect = android.graphics.RectF(rect.left, rect.top, rect.right, rect.bottom)
        val pathSweep = if (centerAngle > 0f) -sweep else sweep
        val pathStart = if (centerAngle > 0f) centerAngle + sweep / 2f else centerAngle - sweep / 2f
        path.addArc(pathRect, pathStart, pathSweep)
        val arcLength = (Math.toRadians(sweep.toDouble()) * radius).toFloat()
        val hOffset = ((arcLength - paint.measureText(label)) / 2f).coerceAtLeast(0f)
        val vOffset = if (centerAngle > 0f) -textSize * 0.22f else textSize * 0.45f
        native.drawTextOnPath(label, path, hOffset, vOffset, paint)
    }
}

private fun lerpColor(start: Color, end: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t,
    )
}

private fun drawEmbossedText(
    native: android.graphics.Canvas,
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    bold: Boolean,
) {
    val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color(0xFF120703).copy(alpha = 0.82f).toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = size
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL,
        )
    }
    val lightPaint = android.graphics.Paint(shadowPaint).apply {
        this.color = Color.White.copy(alpha = 0.26f).toArgb()
    }
    val textPaint = android.graphics.Paint(shadowPaint).apply {
        this.color = color.toArgb()
    }
    native.drawText(text, x + size * 0.04f, y + size * 0.04f, shadowPaint)
    native.drawText(text, x - size * 0.025f, y - size * 0.025f, lightPaint)
    native.drawText(text, x, y, textPaint)
}

private fun monthName(month: Int): String {
    val normalized = Math.floorMod(month - 1, 12) + 1
    return java.time.Month.of(normalized).name.take(3)
}

private fun previousDay(day: Int, year: Int, month: Int): Int =
    if (day <= 1) YearMonth.of(year, month).lengthOfMonth() else day - 1

private fun nextDay(day: Int, year: Int, month: Int): Int {
    val monthLength = YearMonth.of(year, month).lengthOfMonth()
    return if (day >= monthLength) 1 else day + 1
}
