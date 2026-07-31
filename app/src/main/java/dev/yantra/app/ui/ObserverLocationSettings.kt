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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
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
    var privacyOpen by remember { mutableStateOf(false) }
    var manualOpen by remember(current.usesDeviceLocation) { mutableStateOf(!current.usesDeviceLocation) }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var timeZone by remember(current.timeZoneId) { mutableStateOf(current.timeZoneId) }
    var message by remember { mutableStateOf<String?>(null) }
    val copperButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF8E5424),
        contentColor = Color(0xFFFFE8B0),
    )
    val bronzeTextButtonColors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE8CA8B))

    fun applyDeviceLocation() {
        val location = lastKnownLocation(context)
        if (location == null) {
            message = "Location is not available yet. Turn on device location or choose a city."
            manualOpen = true
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
            message = "Using ${next.label}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) applyDeviceLocation() else {
            manualOpen = true
            message = "Permission denied. Choose a city and time zone instead."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Observer location", color = accentColor)
        Text("${current.label} · ${current.timeZoneId}", color = textColor)
        Text("Location is used only for local astronomical timing. Yantra never tracks movement or runs background location.", color = textColor.copy(alpha = 0.72f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { privacyOpen = true }, colors = copperButtonColors) { Text("Use device location") }
            TextButton(onClick = { manualOpen = !manualOpen }, colors = bronzeTextButtonColors) { Text("Choose a city") }
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
                    }, colors = bronzeTextButtonColors) { Text("Allow location") }
                },
                dismissButton = { TextButton(onClick = { privacyOpen = false; manualOpen = true }, colors = bronzeTextButtonColors) { Text("Choose a city") } },
            )
        }
        if (manualOpen) {
            Text("The place name is sent once to the device geocoding provider. The resulting coordinates are cached on this phone.", color = textColor.copy(alpha = 0.72f))
            OutlinedTextField(city, { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state, { state = it }, label = { Text("State / Province") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(country, { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(timeZone, { timeZone = it }, label = { Text("Time zone (for example Asia/Kolkata)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val validZone = runCatching { ZoneId.of(timeZone.trim()) }.getOrNull()
                if (city.isBlank() || country.isBlank() || validZone == null) {
                    message = "Enter a city, country, and valid IANA time zone."
                } else scope.launch {
                    val query = listOf(city, state, country).filter { it.isNotBlank() }.joinToString(", ")
                    val coordinates = geocodePlace(context, query)
                    if (coordinates == null) message = "Location not found. Check the place name and network connection."
                    else {
                        val next = ObserverLocation(coordinates.first, coordinates.second, validZone.id, query, false)
                        saveObserverLocation(context, next)
                        onChanged(next)
                        message = "Using $query"
                        manualOpen = false
                    }
                }
            }, colors = copperButtonColors) { Text("Use this location") }
        }
        message?.let { Text(it, color = accentColor) }
    }
}
