package dev.yantra.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlin.math.min

@Composable
internal fun CryptexDatePicker(
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

private data class RollerValues(
    val previous: String,
    val current: String,
    val next: String,
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

private fun DrawScope.drawCryptexPicker(
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

private fun DrawScope.drawCryptexColumn(
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

private fun Offset.isInRect(rect: Rect): Boolean =
    x in rect.left..rect.right && y in rect.top..rect.bottom

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
