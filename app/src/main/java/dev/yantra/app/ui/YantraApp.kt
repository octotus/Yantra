package dev.yantra.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import dev.yantra.app.R
import dev.yantra.app.calendar.MonthSector
import dev.yantra.app.calendar.YantraCalendarEngine
import dev.yantra.app.calendar.YantraState
import dev.yantra.app.engine.AstronomyEngine
import dev.yantra.app.engine.EphemerisAssets
import dev.yantra.app.engine.Observer
import dev.yantra.app.engine.SwissEphemeris
import kotlinx.coroutines.delay
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
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
    val engine = remember(context) {
        val ephemerisDirectory = EphemerisAssets(context).install()
        YantraCalendarEngine(
            AstronomyEngine(
                longitudeProvider = SwissEphemeris(ephemerisDirectory.absolutePath)
            )
        )
    }
    val observer = remember { Observer(latitude = 45.5019, longitude = -73.5674) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    var datePreviewActive by remember { mutableStateOf(false) }
    val state = remember(now) { engine.compute(now, observer) }
    var lunarEmphasis by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!datePreviewActive) {
                now = ZonedDateTime.now()
            }
            delay(60_000)
        }
    }

    LaunchedEffect(datePreviewActive, now) {
        if (datePreviewActive) {
            delay(5_000)
            datePreviewActive = false
            now = ZonedDateTime.now()
        }
    }

    LaunchedEffect(lunarEmphasis) {
        if (lunarEmphasis) {
            delay(5_000)
            lunarEmphasis = false
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
                        lunarEmphasis = lunarEmphasis,
                        now = now,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize(),
                        onMoonTap = { lunarEmphasis = true },
                        onDateTap = { datePickerOpen = true },
                    )
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
                }
            }
        }
    }
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
    val plateRect: Rect,
)

private fun yantraLayout(width: Float, height: Float): YantraLayout {
    val radius = min(width * 0.455f, height * 0.315f)
    val ringWidth = radius * 0.135f
    val tithiRingWidth = ringWidth * 1.15f
    val tithiRingRadius = radius - ringWidth * 0.35f
    val faceRadius = tithiRingRadius + tithiRingWidth * 0.42f
    val bodyRadius = faceRadius + ringWidth * 0.38f
    val basePlateHeight = ringWidth * 1.62f
    val plateHeight = basePlateHeight * 1.5f
    val plateWidth = min(width * 0.84f, radius * 2.02f)
    val plateOffset = bodyRadius + basePlateHeight * 0.86f + basePlateHeight * 2.0f
    val centerY = min(height * 0.46f, height - plateOffset - plateHeight * 0.5f - 10f)
        .coerceAtLeast(bodyRadius + basePlateHeight * 0.25f)
    val center = Offset(width / 2f, centerY)
    val plateCenter = Offset(center.x, center.y + plateOffset)
    val plateRect = Rect(
        plateCenter.x - plateWidth / 2f,
        plateCenter.y - plateHeight / 2f,
        plateCenter.x + plateWidth / 2f,
        plateCenter.y + plateHeight / 2f,
    )
    return YantraLayout(center, radius, plateRect)
}

@Composable
private fun YantraInstrument(
    state: YantraState,
    sigilImages: SigilImages,
    lunarEmphasis: Boolean,
    now: ZonedDateTime,
    modifier: Modifier = Modifier,
    onMoonTap: () -> Unit,
    onDateTap: () -> Unit,
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
        modifier = modifier.pointerInput(state, now) {
            detectTapGestures { tap ->
                val layout = yantraLayout(size.width.toFloat(), size.height.toFloat())
                val distance = hypot(tap.x - layout.center.x, tap.y - layout.center.y)
                val moonRadius = layout.radius * 0.19f
                if (distance <= moonRadius * 1.6f) {
                    onMoonTap()
                } else if (tap.isInRect(layout.plateRect)) {
                    onDateTap()
                }
            }
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
        val rashiRingRadius = radius - ringWidth * 3.8f
        val faceRadius = tithiRingRadius + tithiRingWidth * 0.42f
        val bodyRadius = faceRadius + ringWidth * 0.38f

        drawPocketWatchBody(center, faceRadius, bodyRadius, ringWidth, brass, brightGold)
        drawDeviceLighting(center, faceRadius, state.solarAltitude.toFloat(), moonlight, brightGold)

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
                sectors = state.monthSectors,
                activeIndex = state.month.index,
                radius = monthRingRadius,
                width = ringWidth,
                inactive = bronze,
                active = activeGold,
            )
            drawRing(12, rashiActiveIndex, rashiRingRadius, ringWidth, bronze, rashiActiveColor.copy(alpha = 0.28f))
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
                radius = rashiRingRadius,
                iconSize = ringWidth * 1.18f,
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
        drawBronzeDatePlate(
            rect = layout.plateRect,
            dateLabel = dateLabel,
            yearLabel = yearLabel,
            gold = brightGold,
        )
    }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBronzeDatePlate(
    rect: Rect,
    dateLabel: String,
    yearLabel: String,
    gold: Color,
) {
    val corner = CornerRadius(rect.height * 0.28f, rect.height * 0.28f)
    drawRoundRect(
        brush = Brush.linearGradient(
            0.0f to Color(0xFF3B2111),
            0.36f to Color(0xFF9A6130),
            0.64f to Color(0xFF4A2814),
            1.0f to Color(0xFFC78342),
            start = Offset(rect.left, rect.top),
            end = Offset(rect.right, rect.bottom),
        ),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        cornerRadius = corner,
    )
    drawRoundRect(
        color = Color(0xFFFFE2A3).copy(alpha = 0.76f),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        cornerRadius = corner,
        style = Stroke(width = 1.3.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFF1A0D06).copy(alpha = 0.5f),
        topLeft = Offset(rect.left + rect.height * 0.1f, rect.top + rect.height * 0.12f),
        size = Size(rect.width - rect.height * 0.2f, rect.height * 0.76f),
        cornerRadius = CornerRadius(rect.height * 0.19f, rect.height * 0.19f),
        style = Stroke(width = 0.8.dp.toPx()),
    )
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        drawEmbossedText(
            native = native,
            text = yearLabel,
            x = rect.center.x,
            y = rect.top + rect.height * 0.42f,
            size = rect.height * 0.27f,
            color = gold,
            bold = true,
        )
        drawEmbossedText(
            native = native,
            text = dateLabel,
            x = rect.center.x,
            y = rect.top + rect.height * 0.75f,
            size = rect.height * 0.28f,
            color = Color(0xFFFFE8B0),
            bold = true,
        )
    }
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
        strokeWidth = width * 0.12f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color.copy(alpha = 0.98f),
        start = handStart,
        end = neck,
        strokeWidth = width * 0.055f,
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
    radius: Float,
    iconSize: Float,
    inactive: Color,
    active: Color,
) {
    val sweep = 360f / 12f
    for (index in 0 until 12) {
        val angle = Math.toRadians((-90f + (index + 0.5f) * sweep).toDouble())
        val position = Offset(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius,
        )
        if (index == activeIndex) {
            drawCircle(Color(0xFF0B0906).copy(alpha = 0.82f), iconSize * 0.66f, position)
            drawCircle(active.copy(alpha = 0.28f), iconSize * 0.78f, position, style = Stroke(width = iconSize * 0.08f))
            drawRashiSigil(index, position, iconSize * 1.28f, active)
        } else {
            drawRashiSigil(index, position, iconSize * 0.9f, inactive)
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
    val waxing = phase >= 180.0
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
            path.moveTo(center.x, center.y - radius)
            for (step in 0..steps) {
                val y = -radius + (2f * radius * step / steps)
                val edge = sqrt((radius * radius - y * y).coerceAtLeast(0f))
                path.lineTo(center.x + edge, center.y + y)
            }
            for (step in steps downTo 0) {
                val y = -radius + (2f * radius * step / steps)
                val edge = sqrt((radius * radius - y * y).coerceAtLeast(0f))
                path.lineTo(center.x + phaseCos * edge, center.y + y)
            }
        } else {
            path.moveTo(center.x, center.y - radius)
            for (step in 0..steps) {
                val y = -radius + (2f * radius * step / steps)
                val edge = sqrt((radius * radius - y * y).coerceAtLeast(0f))
                path.lineTo(center.x - edge, center.y + y)
            }
            for (step in steps downTo 0) {
                val y = -radius + (2f * radius * step / steps)
                val edge = sqrt((radius * radius - y * y).coerceAtLeast(0f))
                path.lineTo(center.x + phaseCos * edge, center.y + y)
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
        90f + index * 12f
    } else {
        -90f + (index - 15) * 12f
    }

private fun tithiCellCenterAngle(index: Int): Float = tithiCellStartAngle(index) + 6f

private fun Offset.isInRect(rect: Rect): Boolean =
    x in rect.left..rect.right && y in rect.top..rect.bottom

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
