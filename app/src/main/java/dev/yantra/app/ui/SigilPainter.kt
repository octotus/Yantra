package dev.yantra.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class SigilImages(
    val maghaCrown: ImageBitmap,
    val pushyaFlower: ImageBitmap,
    val purvaPhalguniPavilion: ImageBitmap,
)

fun DrawScope.drawRashiSigil(
    index: Int,
    center: Offset,
    size: Float,
    color: Color,
) {
    when (index) {
        0 -> drawRam(center, size, color)
        1 -> drawBull(center, size, color)
        2 -> drawPair(center, size, color)
        3 -> drawCrab(center, size, color)
        4 -> drawLion(center, size, color)
        5 -> drawMaiden(center, size, color)
        6 -> drawScales(center, size, color)
        7 -> drawScorpion(center, size, color)
        8 -> drawBow(center, size, color)
        9 -> drawMakara(center, size, color)
        10 -> drawPot(center, size, color)
        11 -> drawFishPair(center, size, color)
    }
}

fun DrawScope.drawNakshatraSigil(
    index: Int,
    center: Offset,
    size: Float,
    color: Color,
    images: SigilImages,
) {
    when (index) {
        0 -> drawHorseHead(center, size, color)
        1 -> drawChalice(center, size, color)
        2 -> drawFlame(center, size, color)
        3 -> drawCartWheel(center, size, color)
        4 -> drawDeerHead(center, size, color)
        5 -> drawDiamond(center, size, color)
        6 -> drawBow(center, size, color)
        7 -> drawTintedImage(images.pushyaFlower, center, size, color)
        8 -> drawCoiledSerpent(center, size, color)
        9 -> drawTintedImage(images.maghaCrown, center, size, color)
        10 -> drawTintedImage(images.purvaPhalguniPavilion, center, size, color)
        11 -> drawKamandalu(center, size, color)
        12 -> drawHand(center, size, color)
        13 -> drawGem(center, size, color)
        14 -> drawSprout(center, size, color)
        15 -> drawArch(center, size, color)
        16 -> drawLotus(center, size, color)
        17 -> drawAmulet(center, size, color)
        18 -> drawRadical(center, size, color)
        19 -> drawFan(center, size, color)
        20 -> drawTusks(center, size, color)
        21 -> drawFootprints(center, size, color)
        22 -> drawDrum(center, size, color)
        23 -> drawCircle(color, size * 0.35f, center, style = Stroke(size * 0.08f))
        24 -> drawMask(center, size, color)
        25 -> drawRubinsVase(center, size, color)
        26 -> drawFishPair(center, size, color)
    }
}

private fun DrawScope.drawTintedImage(image: ImageBitmap, center: Offset, size: Float, color: Color) {
    val px = size.toInt().coerceAtLeast(1)
    drawImage(
        image = image,
        srcOffset = IntOffset(0, 0),
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset((center.x - px / 2f).toInt(), (center.y - px / 2f).toInt()),
        dstSize = IntSize(px, px),
        colorFilter = ColorFilter.tint(color),
    )
}

private fun DrawScope.strokePath(color: Color, width: Float, block: Path.() -> Unit) {
    val path = Path().apply(block)
    drawPath(path, color, style = Stroke(width = width))
}

private fun DrawScope.drawArcBox(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    rect: androidx.compose.ui.geometry.Rect,
    width: Float,
) {
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height),
        style = Stroke(width),
    )
}

private fun DrawScope.drawRam(c: Offset, s: Float, color: Color) {
    val w = s * 0.07f
    drawArcBox(color, 190f, 250f, c.box(s * 0.8f), w)
    drawArcBox(color, -80f, 250f, c.box(s * 0.8f), w)
    drawCircle(color, s * 0.08f, c)
}

private fun DrawScope.drawBull(c: Offset, s: Float, color: Color) {
    val w = s * 0.07f
    drawArcBox(color, 185f, 170f, Offset(c.x - s * 0.45f, c.y - s * 0.3f).box(s * 0.55f), w)
    drawArcBox(color, 185f, 170f, Offset(c.x + s * 0.45f, c.y - s * 0.3f).box(s * 0.55f), w)
    drawOval(color, c.topLeft(s * 0.55f, s * 0.42f), Size(s * 0.55f, s * 0.42f), style = Stroke(w))
}

private fun DrawScope.drawPair(c: Offset, s: Float, color: Color) {
    val w = s * 0.07f
    drawLine(color, c + Offset(-s * 0.22f, -s * 0.35f), c + Offset(-s * 0.22f, s * 0.35f), w)
    drawLine(color, c + Offset(s * 0.22f, -s * 0.35f), c + Offset(s * 0.22f, s * 0.35f), w)
    drawLine(color, c + Offset(-s * 0.35f, -s * 0.35f), c + Offset(s * 0.35f, -s * 0.35f), w)
    drawLine(color, c + Offset(-s * 0.35f, s * 0.35f), c + Offset(s * 0.35f, s * 0.35f), w)
}

private fun DrawScope.drawCrab(c: Offset, s: Float, color: Color) {
    val w = s * 0.07f
    drawArcBox(color, 35f, 290f, Offset(c.x - s * 0.18f, c.y - s * 0.08f).box(s * 0.35f), w)
    drawArcBox(color, 215f, 290f, Offset(c.x + s * 0.18f, c.y + s * 0.08f).box(s * 0.35f), w)
    drawLine(color, c + Offset(-s * 0.42f, -s * 0.22f), c + Offset(s * 0.1f, -s * 0.22f), w)
    drawLine(color, c + Offset(-s * 0.1f, s * 0.22f), c + Offset(s * 0.42f, s * 0.22f), w)
}

private fun DrawScope.drawLion(c: Offset, s: Float, color: Color) {
    drawCircle(color, s * 0.22f, c, style = Stroke(s * 0.07f))
    repeat(10) {
        val a = it * 2f * PI.toFloat() / 10f
        drawLine(color, c.polar(a, s * 0.3f), c.polar(a, s * 0.45f), s * 0.05f)
    }
}

private fun DrawScope.drawMaiden(c: Offset, s: Float, color: Color) {
    val w = s * 0.07f
    drawCircle(color, s * 0.12f, c + Offset(0f, -s * 0.32f), style = Stroke(w))
    drawLine(color, c + Offset(0f, -s * 0.18f), c + Offset(0f, s * 0.3f), w)
    drawLine(color, c + Offset(0f, -s * 0.02f), c + Offset(-s * 0.25f, s * 0.15f), w)
    drawLine(color, c + Offset(0f, -s * 0.02f), c + Offset(s * 0.25f, s * 0.15f), w)
}

private fun DrawScope.drawScales(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawLine(color, c + Offset(0f, -s * 0.38f), c + Offset(0f, s * 0.28f), w)
    drawLine(color, c + Offset(-s * 0.42f, -s * 0.12f), c + Offset(s * 0.42f, -s * 0.12f), w)
    drawArcBox(color, 0f, 180f, Offset(c.x - s * 0.42f, c.y - s * 0.12f).box(s * 0.28f), w)
    drawArcBox(color, 0f, 180f, Offset(c.x + s * 0.42f, c.y - s * 0.12f).box(s * 0.28f), w)
}

private fun DrawScope.drawScorpion(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    strokePath(color, w) {
        moveTo(c.x - s * 0.35f, c.y - s * 0.1f)
        cubicTo(c.x - s * 0.1f, c.y - s * 0.35f, c.x + s * 0.25f, c.y - s * 0.2f, c.x + s * 0.15f, c.y + s * 0.12f)
        cubicTo(c.x + s * 0.05f, c.y + s * 0.4f, c.x + s * 0.35f, c.y + s * 0.38f, c.x + s * 0.42f, c.y + s * 0.12f)
    }
    drawLine(color, c + Offset(s * 0.42f, s * 0.12f), c + Offset(s * 0.32f, s * 0.02f), w)
}

private fun DrawScope.drawBow(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawArcBox(color, -70f, 140f, c.box(s * 0.75f), w)
    drawLine(color, c + Offset(-s * 0.32f, -s * 0.35f), c + Offset(-s * 0.32f, s * 0.35f), w)
    drawLine(color, c + Offset(-s * 0.32f, 0f), c + Offset(s * 0.38f, 0f), w)
}

private fun DrawScope.drawMakara(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    strokePath(color, w) {
        moveTo(c.x - s * 0.35f, c.y)
        cubicTo(c.x - s * 0.1f, c.y - s * 0.35f, c.x + s * 0.35f, c.y - s * 0.25f, c.x + s * 0.28f, c.y + s * 0.08f)
        cubicTo(c.x + s * 0.22f, c.y + s * 0.32f, c.x - s * 0.15f, c.y + s * 0.32f, c.x - s * 0.05f, c.y + s * 0.05f)
    }
}

private fun DrawScope.drawPot(c: Offset, s: Float, color: Color) = drawKamandalu(c, s, color)

private fun DrawScope.drawFishPair(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawFish(c + Offset(-s * 0.12f, -s * 0.12f), s * 0.48f, color, w, false)
    drawFish(c + Offset(s * 0.12f, s * 0.12f), s * 0.48f, color, w, true)
}

private fun DrawScope.drawFish(c: Offset, s: Float, color: Color, w: Float, flip: Boolean) {
    val dir = if (flip) -1f else 1f
    drawOval(color, c.topLeft(s * 0.7f, s * 0.35f), Size(s * 0.7f, s * 0.35f), style = Stroke(w))
    drawLine(color, c + Offset(dir * s * 0.35f, 0f), c + Offset(dir * s * 0.55f, -s * 0.18f), w)
    drawLine(color, c + Offset(dir * s * 0.35f, 0f), c + Offset(dir * s * 0.55f, s * 0.18f), w)
}

private fun DrawScope.drawHorseHead(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    strokePath(color, w) {
        moveTo(c.x - s * 0.1f, c.y - s * 0.38f)
        cubicTo(c.x + s * 0.2f, c.y - s * 0.3f, c.x + s * 0.3f, c.y, c.x + s * 0.12f, c.y + s * 0.34f)
        lineTo(c.x - s * 0.22f, c.y + s * 0.25f)
        cubicTo(c.x - s * 0.05f, c.y, c.x - s * 0.28f, c.y - s * 0.18f, c.x - s * 0.1f, c.y - s * 0.38f)
    }
}

private fun DrawScope.drawChalice(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawArcBox(color, 0f, 180f, c.box(s * 0.65f), w)
    drawLine(color, c + Offset(0f, s * 0.1f), c + Offset(0f, s * 0.38f), w)
    drawLine(color, c + Offset(-s * 0.22f, s * 0.38f), c + Offset(s * 0.22f, s * 0.38f), w)
}

private fun DrawScope.drawFlame(c: Offset, s: Float, color: Color) {
    strokePath(color, s * 0.06f) {
        moveTo(c.x, c.y + s * 0.42f)
        cubicTo(c.x - s * 0.35f, c.y + s * 0.05f, c.x - s * 0.05f, c.y - s * 0.16f, c.x, c.y - s * 0.42f)
        cubicTo(c.x + s * 0.34f, c.y - s * 0.12f, c.x + s * 0.32f, c.y + s * 0.18f, c.x, c.y + s * 0.42f)
    }
}

private fun DrawScope.drawCartWheel(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawCircle(color, s * 0.38f, c, style = Stroke(w))
    drawCircle(color, s * 0.09f, c, style = Stroke(w))
    repeat(8) {
        val a = it * PI.toFloat() / 4f
        drawLine(color, c.polar(a, s * 0.12f), c.polar(a, s * 0.38f), w)
    }
}

private fun DrawScope.drawDeerHead(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawOval(color, c.topLeft(s * 0.35f, s * 0.55f), Size(s * 0.35f, s * 0.55f), style = Stroke(w))
    drawLine(color, c + Offset(-s * 0.12f, -s * 0.25f), c + Offset(-s * 0.35f, -s * 0.45f), w)
    drawLine(color, c + Offset(s * 0.12f, -s * 0.25f), c + Offset(s * 0.35f, -s * 0.45f), w)
    drawLine(color, c + Offset(-s * 0.3f, -s * 0.42f), c + Offset(-s * 0.42f, -s * 0.25f), w)
    drawLine(color, c + Offset(s * 0.3f, -s * 0.42f), c + Offset(s * 0.42f, -s * 0.25f), w)
}

private fun DrawScope.drawDiamond(c: Offset, s: Float, color: Color) {
    strokePath(color, s * 0.06f) {
        moveTo(c.x, c.y - s * 0.42f)
        lineTo(c.x + s * 0.36f, c.y)
        lineTo(c.x, c.y + s * 0.42f)
        lineTo(c.x - s * 0.36f, c.y)
        close()
    }
}

private fun DrawScope.drawCoiledSerpent(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawArcBox(color, 15f, 300f, c.box(s * 0.72f), w)
    drawArcBox(color, 35f, 260f, c.box(s * 0.42f), w)
    drawCircle(color, s * 0.04f, c + Offset(s * 0.3f, -s * 0.16f))
}

private fun DrawScope.drawKamandalu(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawLine(color, c + Offset(-s * 0.2f, -s * 0.42f), c + Offset(s * 0.2f, -s * 0.42f), w)
    drawOval(color, Offset(c.x - s * 0.275f, c.y - s * 0.12f), Size(s * 0.55f, s * 0.65f), style = Stroke(w))
    drawArcBox(color, -75f, 150f, Offset(c.x + s * 0.35f, c.y + s * 0.08f).box(s * 0.28f), w)
}

private fun DrawScope.drawHand(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    repeat(4) {
        val x = c.x - s * 0.18f + it * s * 0.12f
        drawLine(color, Offset(x, c.y - s * 0.35f), Offset(x, c.y + s * 0.08f), w)
    }
    drawArcBox(color, 20f, 250f, Offset(c.x - s * 0.05f, c.y + s * 0.1f).box(s * 0.45f), w)
}

private fun DrawScope.drawGem(c: Offset, s: Float, color: Color) {
    drawDiamond(c, s, color)
    drawLine(color, c + Offset(-s * 0.28f, 0f), c + Offset(s * 0.28f, 0f), s * 0.05f)
}

private fun DrawScope.drawSprout(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawLine(color, c + Offset(0f, s * 0.42f), c + Offset(0f, -s * 0.1f), w)
    drawArcBox(color, 190f, 160f, Offset(c.x - s * 0.22f, c.y - s * 0.25f).box(s * 0.42f), w)
    drawArcBox(color, -170f, 160f, Offset(c.x + s * 0.22f, c.y - s * 0.25f).box(s * 0.42f), w)
}

private fun DrawScope.drawArch(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawArcBox(color, 180f, 180f, c.box(s * 0.75f), w)
    drawLine(color, c + Offset(-s * 0.38f, 0f), c + Offset(-s * 0.38f, s * 0.42f), w)
    drawLine(color, c + Offset(s * 0.38f, 0f), c + Offset(s * 0.38f, s * 0.42f), w)
}

private fun DrawScope.drawLotus(c: Offset, s: Float, color: Color) {
    val w = s * 0.05f
    repeat(5) {
        rotate((it - 2) * 22f, c) {
            drawOval(color, Offset(c.x - s * 0.1f, c.y - s * 0.42f), Size(s * 0.2f, s * 0.65f), style = Stroke(w))
        }
    }
    drawArcBox(color, 20f, 140f, androidx.compose.ui.geometry.Rect(c.x - s * 0.375f, c.y - s * 0.06f, c.x + s * 0.375f, c.y + s * 0.69f), w)
}

private fun DrawScope.drawAmulet(c: Offset, s: Float, color: Color) {
    drawCircle(color, s * 0.32f, c, style = Stroke(s * 0.06f))
    drawLine(color, c + Offset(0f, -s * 0.32f), c + Offset(0f, s * 0.32f), s * 0.05f)
    drawLine(color, c + Offset(-s * 0.32f, 0f), c + Offset(s * 0.32f, 0f), s * 0.05f)
}

private fun DrawScope.drawRadical(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawLine(color, c + Offset(-s * 0.4f, s * 0.15f), c + Offset(-s * 0.15f, s * 0.15f), w)
    drawLine(color, c + Offset(-s * 0.15f, s * 0.15f), c + Offset(0f, s * 0.4f), w)
    drawLine(color, c + Offset(0f, s * 0.4f), c + Offset(s * 0.28f, -s * 0.38f), w)
    drawLine(color, c + Offset(s * 0.28f, -s * 0.38f), c + Offset(s * 0.45f, -s * 0.38f), w)
}

private fun DrawScope.drawFan(c: Offset, s: Float, color: Color) {
    val w = s * 0.05f
    repeat(6) {
        val a = (-130 + it * 26) * PI.toFloat() / 180f
        drawLine(color, c + Offset(0f, s * 0.35f), c + Offset(cos(a), sin(a)) * s * 0.45f, w)
    }
    drawArcBox(color, 205f, 130f, c.box(s * 0.9f), w)
}

private fun DrawScope.drawTusks(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    strokePath(color, w) {
        moveTo(c.x - s * 0.22f, c.y - s * 0.42f)
        cubicTo(c.x - s * 0.28f, c.y, c.x - s * 0.22f, c.y + s * 0.28f, c.x - s * 0.48f, c.y + s * 0.42f)
    }
    strokePath(color, w) {
        moveTo(c.x + s * 0.22f, c.y - s * 0.42f)
        cubicTo(c.x + s * 0.28f, c.y, c.x + s * 0.22f, c.y + s * 0.28f, c.x + s * 0.48f, c.y + s * 0.42f)
    }
}

private fun DrawScope.drawFootprints(c: Offset, s: Float, color: Color) {
    val positions = listOf(Offset(-0.22f, -0.12f), Offset(0.12f, 0.04f), Offset(-0.08f, 0.28f))
    positions.forEach { p ->
        drawOval(color, Offset(c.x + p.x * s - s * 0.08f, c.y + p.y * s - s * 0.13f), Size(s * 0.16f, s * 0.26f), style = Stroke(s * 0.045f))
    }
}

private fun DrawScope.drawDrum(c: Offset, s: Float, color: Color) {
    val w = s * 0.06f
    drawOval(color, Offset(c.x - s * 0.275f, c.y - s * 0.35f), Size(s * 0.55f, s * 0.22f), style = Stroke(w))
    drawOval(color, Offset(c.x - s * 0.275f, c.y + s * 0.13f), Size(s * 0.55f, s * 0.22f), style = Stroke(w))
    drawLine(color, c + Offset(-s * 0.27f, -s * 0.24f), c + Offset(s * 0.27f, s * 0.24f), w)
    drawLine(color, c + Offset(s * 0.27f, -s * 0.24f), c + Offset(-s * 0.27f, s * 0.24f), w)
}

private fun DrawScope.drawMask(c: Offset, s: Float, color: Color) {
    val w = s * 0.05f
    drawOval(color, c.topLeft(s * 0.65f, s * 0.5f), Size(s * 0.65f, s * 0.5f), style = Stroke(w))
    drawLine(color, c + Offset(0f, -s * 0.23f), c + Offset(0f, s * 0.23f), w)
    drawCircle(color, s * 0.04f, c + Offset(-s * 0.15f, -s * 0.04f))
    drawCircle(color, s * 0.04f, c + Offset(s * 0.15f, -s * 0.04f))
}

private fun DrawScope.drawRubinsVase(c: Offset, s: Float, color: Color) {
    strokePath(color, s * 0.055f) {
        moveTo(c.x - s * 0.34f, c.y - s * 0.42f)
        cubicTo(c.x - s * 0.18f, c.y - s * 0.18f, c.x - s * 0.18f, c.y + s * 0.1f, c.x - s * 0.34f, c.y + s * 0.42f)
        moveTo(c.x + s * 0.34f, c.y - s * 0.42f)
        cubicTo(c.x + s * 0.18f, c.y - s * 0.18f, c.x + s * 0.18f, c.y + s * 0.1f, c.x + s * 0.34f, c.y + s * 0.42f)
    }
    drawLine(color, c + Offset(-s * 0.2f, -s * 0.42f), c + Offset(s * 0.2f, -s * 0.42f), s * 0.055f)
    drawLine(color, c + Offset(-s * 0.22f, s * 0.42f), c + Offset(s * 0.22f, s * 0.42f), s * 0.055f)
}

private fun Offset.box(side: Float): androidx.compose.ui.geometry.Rect =
    androidx.compose.ui.geometry.Rect(x - side / 2f, y - side / 2f, x + side / 2f, y + side / 2f)

private fun Offset.topLeft(width: Float, height: Float): Offset =
    Offset(x - width / 2f, y - height / 2f)

private fun Offset.polar(angle: Float, radius: Float): Offset =
    this + Offset(cos(angle) * radius, sin(angle) * radius)

private operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)

private operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)
