package dev.yantra.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.yantra.app.engine.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.Locale

private const val OBSERVER_PREFS = "yantra_observer"

internal data class ObserverLocation(
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val label: String,
    val usesDeviceLocation: Boolean,
) {
    val observer: Observer get() = Observer(latitude, longitude)
    val zoneId: ZoneId get() = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
}

internal fun loadObserverLocation(context: Context): ObserverLocation {
    val prefs = context.getSharedPreferences(OBSERVER_PREFS, Context.MODE_PRIVATE)
    return ObserverLocation(
        latitude = prefs.getString("latitude", null)?.toDoubleOrNull() ?: 45.5019,
        longitude = prefs.getString("longitude", null)?.toDoubleOrNull() ?: -73.5674,
        timeZoneId = prefs.getString("time_zone", null) ?: ZoneId.systemDefault().id,
        label = prefs.getString("label", null) ?: "Device location",
        usesDeviceLocation = prefs.getBoolean("uses_device", true),
    )
}

internal fun saveObserverLocation(context: Context, value: ObserverLocation) {
    context.getSharedPreferences(OBSERVER_PREFS, Context.MODE_PRIVATE).edit()
        .putString("latitude", value.latitude.toString())
        .putString("longitude", value.longitude.toString())
        .putString("time_zone", value.timeZoneId)
        .putString("label", value.label)
        .putBoolean("uses_device", value.usesDeviceLocation)
        .apply()
}

private fun lastKnownLocation(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.getProviders(true)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}

@Suppress("DEPRECATION")
private suspend fun reverseLabel(context: Context, latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
    runCatching {
        val address = Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)?.firstOrNull()
        listOfNotNull(address?.locality, address?.adminArea, address?.countryName).distinct().joinToString(", ")
    }.getOrNull().orEmpty().ifBlank { "${"%.3f".format(latitude)}, ${"%.3f".format(longitude)}" }
}

@Suppress("DEPRECATION")
private suspend fun geocodePlace(context: Context, query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    runCatching {
        Geocoder(context, Locale.getDefault()).getFromLocationName(query, 1)?.firstOrNull()?.let { it.latitude to it.longitude }
    }.getOrNull()
}

@Composable
internal fun ObserverLocationPanel(
    current: ObserverLocation,
    textColor: Color,
    accentColor: Color,
    onChanged: (ObserverLocation) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var privacyOpen by remember { mutableStateOf(false) }
    var manualOpen by remember { mutableStateOf(!current.usesDeviceLocation) }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var timeZone by remember(current.timeZoneId) { mutableStateOf(current.timeZoneId) }
    var message by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }
    var countryError by remember { mutableStateOf<String?>(null) }
    var timeZoneError by remember { mutableStateOf<String?>(null) }
    var placeError by remember { mutableStateOf<String?>(null) }
    val ivory = Color(0xFFFFE8B0)
    val brightGold = Color(0xFFFFE2A3)
    val copper = Color(0xFF8E5424)
    val deepCopper = Color(0xFF211007)
    val ink = Color(0xFF050302)
    val errorColor = Color(0xFFFFB4A9)
    val copperButtonColors = ButtonDefaults.buttonColors(
        containerColor = copper,
        contentColor = ivory,
    )
    val inactiveButtonColors = ButtonDefaults.buttonColors(
        containerColor = deepCopper,
        contentColor = brightGold,
    )
    val confirmButtonColors = ButtonDefaults.buttonColors(
        containerColor = brightGold,
        contentColor = ink,
    )
    val dialogTextButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ivory,
        unfocusedTextColor = ivory,
        focusedLabelColor = brightGold,
        unfocusedLabelColor = ivory.copy(alpha = 0.88f),
        cursorColor = brightGold,
        focusedBorderColor = brightGold,
        unfocusedBorderColor = ivory.copy(alpha = 0.7f),
        focusedContainerColor = ink.copy(alpha = 0.78f),
        unfocusedContainerColor = deepCopper.copy(alpha = 0.72f),
        errorTextColor = errorColor,
        errorLabelColor = errorColor,
        errorBorderColor = errorColor,
        errorCursorColor = errorColor,
        errorSupportingTextColor = errorColor,
    )

    fun openManualEntry() {
        manualOpen = true
        privacyOpen = false
        timeZone = ""
        message = null
        cityError = null
        countryError = null
        timeZoneError = null
        placeError = null
    }

    fun submitManualLocation() {
        cityError = if (city.isBlank()) "Enter a city." else null
        countryError = if (country.isBlank()) "Enter a country." else null
        val validZone = runCatching { ZoneId.of(timeZone.trim()) }.getOrNull()
        timeZoneError = when {
            timeZone.isBlank() -> "Enter an IANA time zone, such as America/Toronto."
            validZone == null -> "This time zone is not valid. Use a name such as America/Toronto."
            else -> null
        }
        placeError = null
        if (cityError != null || countryError != null || timeZoneError != null || validZone == null) {
            message = "Please correct the highlighted fields."
            return
        }
        scope.launch {
            val query = listOf(city, state, country).map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
            val coordinates = geocodePlace(context, query)
            if (coordinates == null) {
                placeError = "Location not found. Check the city, state or province, country, and network connection."
                message = null
            } else {
                val next = ObserverLocation(coordinates.first, coordinates.second, validZone.id, query, false)
                saveObserverLocation(context, next)
                onChanged(next)
                message = "Using $query"
                manualOpen = false
                keyboardController?.hide()
            }
        }
    }

    fun applyDeviceLocation() {
        val location = lastKnownLocation(context)
        if (location == null) {
            openManualEntry()
            message = "Location is not available yet. Turn on device location or choose a city."
            return
        }
        scope.launch {
            val next = ObserverLocation(
                location.latitude,
                location.longitude,
                ZoneId.systemDefault().id,
                reverseLabel(context, location.latitude, location.longitude),
                true,
            )
            saveObserverLocation(context, next)
            onChanged(next)
            manualOpen = false
            message = "Using ${next.label}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) applyDeviceLocation() else {
            openManualEntry()
            message = "Permission denied. Choose a city and time zone instead."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Observer location", color = accentColor)
        Text("${current.label} · ${current.timeZoneId}", color = textColor)
        Text("Location is used only for local astronomical timing. Yantra never tracks movement or runs background location.", color = textColor.copy(alpha = 0.88f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { privacyOpen = true },
                colors = if (current.usesDeviceLocation && !manualOpen) copperButtonColors else inactiveButtonColors,
            ) { Text("Use device location") }
            Button(
                onClick = ::openManualEntry,
                colors = if (!current.usesDeviceLocation || manualOpen) copperButtonColors else inactiveButtonColors,
            ) { Text("Choose a city") }
        }
        if (privacyOpen) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { privacyOpen = false },
                title = { Text("Set your local astronomical time") },
                text = { Text("Yantra uses foreground location to calculate local astronomical timings and select the appropriate time zone. It never tracks you and does not run background location. A one-time external lookup may identify the displayed city. Yantra does not operate a server that collects or retains location data.") },
                confirmButton = {
                    TextButton(onClick = {
                        privacyOpen = false
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }, colors = dialogTextButtonColors) { Text("Allow location") }
                },
                dismissButton = { TextButton(onClick = ::openManualEntry, colors = dialogTextButtonColors) { Text("Choose a city") } },
            )
        }
        if (manualOpen) {
            Text("The place name is sent once to the device geocoding provider. The resulting coordinates are cached on this phone.", color = textColor.copy(alpha = 0.88f))
            OutlinedTextField(
                value = city,
                onValueChange = { city = it; cityError = null; placeError = null },
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = cityError != null,
                supportingText = cityError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = state,
                onValueChange = { state = it; placeError = null },
                label = { Text("State / Province (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = country,
                onValueChange = { country = it; countryError = null; placeError = null },
                label = { Text("Country") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = countryError != null,
                supportingText = countryError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = timeZone,
                onValueChange = { timeZone = it; timeZoneError = null },
                label = { Text("Time zone (for example America/Toronto)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = timeZoneError != null,
                supportingText = timeZoneError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitManualLocation() }),
                colors = fieldColors,
            )
            placeError?.let { Text(it, color = errorColor) }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = ::submitManualLocation, colors = confirmButtonColors) { Text("Use this location") }
            }
        }
        message?.let { Text(it, color = if (it.startsWith("Using ")) brightGold else errorColor) }
    }
}
