package dev.yantra.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.yantra.app.calendar.CalendarCatalog
import dev.yantra.app.calendar.MonthNameSet
import dev.yantra.app.calendar.MonthReckoning
import dev.yantra.app.calendar.YantraState

@Composable
internal fun SpecialDayEditor(
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
internal fun YantraSettingsScreen(
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
                Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChanged)
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
