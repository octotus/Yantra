package dev.yantra.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Figurative line art laid over a rashi's stellar pattern. Coordinates are normalized to a square. */
internal fun DrawScope.drawZodiacFigureOutline(
    index: Int,
    center: Offset,
    size: Float,
    color: Color,
    strokeWidth: Float,
) {
    fun p(x: Float, y: Float) = Offset(center.x + x * size, center.y + y * size)
    fun Path.moveTo(point: Offset) = moveTo(point.x, point.y)
    fun Path.lineTo(point: Offset) = lineTo(point.x, point.y)
    fun Path.cubicTo(control1: Offset, control2: Offset, end: Offset) =
        cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
    fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), color, style = Stroke(strokeWidth))
    fun oval(cx: Float, cy: Float, width: Float, height: Float) = drawOval(
        color,
        topLeft = p(cx - width / 2f, cy - height / 2f),
        size = Size(width * size, height * size),
        style = Stroke(strokeWidth),
    )
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, p(x1, y1), p(x2, y2), strokeWidth)

    when (index) {
        0 -> { // Mesha: ram
            oval(0.05f, 0.02f, 0.58f, 0.34f)
            oval(-0.32f, -0.12f, 0.24f, 0.22f)
            path { moveTo(p(-0.38f, -0.22f)); cubicTo(p(-0.55f, -0.42f), p(-0.62f, -0.04f), p(-0.39f, 0.01f)) }
            path { moveTo(p(-0.28f, -0.23f)); cubicTo(p(-0.16f, -0.43f), p(0.00f, -0.19f), p(-0.20f, -0.08f)) }
            line(-0.12f, 0.16f, -0.17f, 0.43f); line(0.19f, 0.16f, 0.25f, 0.43f)
            line(-0.17f, 0.43f, -0.26f, 0.43f); line(0.25f, 0.43f, 0.35f, 0.43f)
            path { moveTo(p(0.34f, -0.05f)); cubicTo(p(0.51f, -0.13f), p(0.54f, 0.02f), p(0.43f, 0.09f)) }
        }
        1 -> { // Vrishabha: bull
            oval(0.02f, 0.02f, 0.62f, 0.34f); oval(-0.35f, -0.08f, 0.25f, 0.24f)
            path { moveTo(p(-0.42f, -0.19f)); cubicTo(p(-0.58f, -0.34f), p(-0.62f, -0.08f), p(-0.45f, -0.04f)) }
            path { moveTo(p(-0.30f, -0.19f)); cubicTo(p(-0.18f, -0.36f), p(-0.08f, -0.13f), p(-0.24f, -0.04f)) }
            line(-0.16f, 0.16f, -0.20f, 0.44f); line(0.23f, 0.16f, 0.28f, 0.44f)
            line(-0.20f, 0.44f, -0.30f, 0.44f); line(0.28f, 0.44f, 0.38f, 0.44f)
            path { moveTo(p(0.34f, -0.05f)); cubicTo(p(0.55f, -0.30f), p(0.57f, 0.03f), p(0.46f, 0.13f)) }
        }
        2 -> { // Mithuna: twins
            for (side in listOf(-1f, 1f)) {
                val x = side * 0.20f
                oval(x, -0.30f, 0.17f, 0.17f)
                line(x, -0.21f, x, 0.22f); line(x, -0.08f, x + side * 0.20f, 0.06f)
                line(x, 0.22f, x + side * 0.13f, 0.47f); line(x, 0.22f, x - side * 0.10f, 0.47f)
            }
            path { moveTo(p(-0.20f, -0.04f)); cubicTo(p(-0.08f, -0.16f), p(0.08f, -0.16f), p(0.20f, -0.04f)) }
        }
        3 -> { // Karka: crab
            oval(0f, 0.08f, 0.55f, 0.30f)
            for (side in listOf(-1f, 1f)) {
                line(side * 0.22f, -0.02f, side * 0.44f, -0.20f)
                line(side * 0.22f, 0.08f, side * 0.47f, 0.02f)
                line(side * 0.20f, 0.18f, side * 0.42f, 0.28f)
                path {
                    moveTo(p(side * 0.43f, -0.20f));
                    cubicTo(p(side * 0.58f, -0.36f), p(side * 0.62f, -0.05f), p(side * 0.44f, -0.04f))
                }
            }
            oval(-0.12f, -0.02f, 0.05f, 0.05f); oval(0.12f, -0.02f, 0.05f, 0.05f)
        }
        4 -> { // Simha: lion
            oval(-0.27f, -0.12f, 0.34f, 0.38f); oval(-0.27f, -0.12f, 0.20f, 0.22f)
            path { moveTo(p(-0.10f, -0.05f)); cubicTo(p(0.04f, -0.27f), p(0.35f, -0.25f), p(0.40f, 0.03f)); lineTo(p(0.34f, 0.24f)); lineTo(p(-0.05f, 0.22f)); close() }
            line(0.24f, 0.20f, 0.29f, 0.47f); line(-0.02f, 0.20f, -0.08f, 0.47f)
            path { moveTo(p(0.39f, 0.02f)); cubicTo(p(0.62f, -0.09f), p(0.58f, -0.38f), p(0.45f, -0.39f)) }
            oval(0.45f, -0.42f, 0.09f, 0.09f)
        }
        5 -> { // Kanya: maiden with grain
            oval(-0.08f, -0.34f, 0.17f, 0.17f)
            path { moveTo(p(-0.09f, -0.24f)); lineTo(p(-0.29f, 0.40f)); lineTo(p(0.20f, 0.40f)); lineTo(p(0.04f, -0.24f)); close() }
            line(-0.02f, -0.10f, 0.31f, 0.10f); line(0.31f, 0.10f, 0.38f, 0.42f)
            for (y in listOf(0.16f, 0.24f, 0.32f)) {
                line(0.38f, y, 0.47f, y - 0.06f); line(0.38f, y + 0.02f, 0.29f, y - 0.04f)
            }
        }
        6 -> { // Tula: scales
            line(0f, -0.36f, 0f, 0.38f); line(-0.43f, -0.18f, 0.43f, -0.18f)
            line(-0.34f, -0.18f, -0.45f, 0.14f); line(-0.16f, -0.18f, -0.05f, 0.14f)
            line(0.34f, -0.18f, 0.45f, 0.14f); line(0.16f, -0.18f, 0.05f, 0.14f)
            path { moveTo(p(-0.46f, 0.14f)); cubicTo(p(-0.36f, 0.30f), p(-0.14f, 0.30f), p(-0.04f, 0.14f)) }
            path { moveTo(p(0.04f, 0.14f)); cubicTo(p(0.14f, 0.30f), p(0.36f, 0.30f), p(0.46f, 0.14f)) }
            line(-0.24f, 0.38f, 0.24f, 0.38f)
        }
        7 -> { // Vrischika: scorpion
            oval(-0.08f, 0.10f, 0.30f, 0.42f)
            for (y in listOf(-0.02f, 0.08f, 0.18f)) {
                line(-0.20f, y, -0.43f, y - 0.12f); line(0.05f, y, 0.30f, y - 0.12f)
            }
            path { moveTo(p(-0.18f, -0.12f)); cubicTo(p(-0.48f, -0.42f), p(-0.58f, -0.08f), p(-0.38f, 0.01f)) }
            path { moveTo(p(0.02f, -0.14f)); cubicTo(p(0.35f, -0.43f), p(0.57f, -0.15f), p(0.32f, 0.02f)) }
            path { moveTo(p(0.02f, 0.29f)); cubicTo(p(0.16f, 0.51f), p(0.48f, 0.36f), p(0.37f, 0.12f)); lineTo(p(0.48f, 0.04f)) }
        }
        8 -> { // Dhanu: archer
            oval(-0.24f, -0.31f, 0.15f, 0.15f); line(-0.24f, -0.23f, -0.12f, 0.17f)
            line(-0.18f, -0.09f, 0.18f, -0.02f); line(-0.12f, 0.17f, -0.32f, 0.46f); line(-0.12f, 0.17f, 0.08f, 0.45f)
            path { moveTo(p(0.31f, -0.39f)); cubicTo(p(0.53f, -0.08f), p(0.49f, 0.28f), p(0.22f, 0.43f)) }
            line(0.31f, -0.39f, 0.22f, 0.43f); line(-0.04f, 0.03f, 0.43f, -0.17f)
            line(0.43f, -0.17f, 0.34f, -0.19f); line(0.43f, -0.17f, 0.38f, -0.09f)
        }
        9 -> { // Makara: sea-goat
            oval(-0.22f, -0.20f, 0.27f, 0.23f)
            path { moveTo(p(-0.33f, -0.28f)); cubicTo(p(-0.51f, -0.48f), p(-0.58f, -0.12f), p(-0.37f, -0.10f)) }
            path { moveTo(p(-0.12f, -0.28f)); cubicTo(p(0.03f, -0.47f), p(0.12f, -0.20f), p(-0.07f, -0.10f)) }
            path { moveTo(p(-0.10f, -0.08f)); cubicTo(p(0.18f, -0.07f), p(0.34f, 0.10f), p(0.25f, 0.28f)); cubicTo(p(0.17f, 0.45f), p(0.38f, 0.48f), p(0.50f, 0.30f)) }
            line(0.50f, 0.30f, 0.44f, 0.12f); line(0.50f, 0.30f, 0.31f, 0.35f)
            line(-0.18f, -0.07f, -0.28f, 0.27f)
        }
        10 -> { // Kumbha: water bearer
            oval(-0.17f, -0.34f, 0.16f, 0.16f); line(-0.17f, -0.25f, -0.06f, 0.18f)
            line(-0.12f, -0.10f, 0.21f, 0.02f); line(-0.06f, 0.18f, -0.24f, 0.46f); line(-0.06f, 0.18f, 0.12f, 0.46f)
            path { moveTo(p(0.14f, -0.12f)); lineTo(p(0.43f, -0.05f)); lineTo(p(0.36f, 0.20f)); lineTo(p(0.10f, 0.13f)); close() }
            path { moveTo(p(0.36f, 0.20f)); cubicTo(p(0.48f, 0.28f), p(0.34f, 0.33f), p(0.45f, 0.41f)) }
            path { moveTo(p(0.29f, 0.21f)); cubicTo(p(0.39f, 0.29f), p(0.25f, 0.35f), p(0.36f, 0.44f)) }
        }
        11 -> { // Meena: paired fish
            fun fish(cx: Float, cy: Float, direction: Float) {
                oval(cx, cy, 0.48f, 0.20f)
                line(cx - direction * 0.24f, cy, cx - direction * 0.39f, cy - 0.14f)
                line(cx - direction * 0.24f, cy, cx - direction * 0.39f, cy + 0.14f)
                drawCircle(color, strokeWidth * 0.9f, p(cx + direction * 0.14f, cy - 0.02f))
            }
            fish(-0.11f, -0.17f, 1f); fish(0.11f, 0.17f, -1f)
            path { moveTo(p(-0.28f, -0.02f)); cubicTo(p(-0.43f, 0.10f), p(-0.38f, 0.31f), p(-0.20f, 0.35f)) }
            path { moveTo(p(0.28f, 0.02f)); cubicTo(p(0.43f, -0.10f), p(0.38f, -0.31f), p(0.20f, -0.35f)) }
        }
    }
}
