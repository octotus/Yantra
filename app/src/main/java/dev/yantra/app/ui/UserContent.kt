package dev.yantra.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.yantra.app.calendar.CalendarCatalog
import dev.yantra.app.calendar.MonthNameSet
import dev.yantra.app.calendar.MonthReckoning
import dev.yantra.app.calendar.YantraState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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

private const val SPECIAL_DAYS_PREFS = "yantra_special_days"
private const val SPECIAL_DAYS_KEY = "days"
private const val USER_SETTINGS_PREFS = "yantra_user_settings"
private const val USER_EVENTS_KEY = "events_json"
private const val USER_LOGO_KEY = "logo_path"
private const val USER_LOGO_FILE = "user_logo"
private const val MONTH_NAME_SET_KEY = "month_name_set"
private const val CALENDAR_LOCALE_RULE_KEY = "calendar_locale_rule"
private const val MONTH_RECKONING_KEY = "month_reckoning"
private const val YANTRA_REPOSITORY_URL = "https://github.com/octotus/Yantra"

internal fun YantraState.toSpecialDay(name: String): SpecialDay =
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

internal fun saveSpecialDays(context: Context, days: List<SpecialDay>) {
    val encoded = days.map { day ->
        listOf(Uri.encode(day.name), day.month, day.paksha, day.tithiNumber.toString()).joinToString("|")
    }.toSet()
    context.getSharedPreferences(SPECIAL_DAYS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putStringSet(SPECIAL_DAYS_KEY, encoded)
        .apply()
}

internal fun selectedMonthNameSet(id: String): MonthNameSet =
    CalendarCatalog.monthNameSets.firstOrNull { it.id == id } ?: CalendarCatalog.monthNameSets.first()

internal fun selectedCalendarLocaleRule(id: String) =
    CalendarCatalog.calendarLocaleRules.firstOrNull { it.id == id } ?: CalendarCatalog.calendarLocaleRules.first()

internal fun selectedMonthReckoning(id: String): MonthReckoning =
    MonthReckoning.values().firstOrNull { it.id == id } ?: MonthReckoning.Amanta

internal fun loadMonthNameSetId(context: Context): String =
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getString(MONTH_NAME_SET_KEY, CalendarCatalog.monthNameSets.first().id)
        ?: CalendarCatalog.monthNameSets.first().id

internal fun saveMonthNameSetId(context: Context, id: String) {
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(MONTH_NAME_SET_KEY, id)
        .apply()
}

internal fun loadCalendarLocaleRuleId(context: Context): String =
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getString(CALENDAR_LOCALE_RULE_KEY, CalendarCatalog.calendarLocaleRules.first().id)
        ?: CalendarCatalog.calendarLocaleRules.first().id

internal fun saveCalendarLocaleRuleId(context: Context, id: String) {
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(CALENDAR_LOCALE_RULE_KEY, id)
        .apply()
}

internal fun loadMonthReckoningId(context: Context): String =
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getString(MONTH_RECKONING_KEY, MonthReckoning.Amanta.id)
        ?: MonthReckoning.Amanta.id

internal fun saveMonthReckoningId(context: Context, id: String) {
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

internal fun saveUserEvents(context: Context, events: List<UserEvent>) {
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

internal fun parseUserEvents(raw: String): List<UserEvent> {
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

internal fun loadUserLogoPath(context: Context): String? =
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getString(USER_LOGO_KEY, null)
        ?.takeIf { File(it).exists() }

internal fun saveUserLogoPath(context: Context, path: String?) {
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(USER_LOGO_KEY, path)
        .apply()
}

internal fun saveUserLogo(context: Context, uri: Uri): String {
    val destination = File(context.filesDir, USER_LOGO_FILE)
    context.contentResolver.openInputStream(uri)?.use { input ->
        destination.outputStream().use { output -> input.copyTo(output) }
    }
    return destination.absolutePath
}

internal fun clearUserLogo(context: Context) {
    File(context.filesDir, USER_LOGO_FILE).delete()
    context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .remove(USER_LOGO_KEY)
        .apply()
}

internal fun loadLogoBitmap(path: String): ImageBitmap? =
    runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()

internal fun openRepository(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YANTRA_REPOSITORY_URL))
    runCatching { context.startActivity(intent) }
}
