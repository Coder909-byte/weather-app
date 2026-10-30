package com.aeris.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aeris.weather.domain.model.WeatherKind
import com.aeris.weather.ui.theme.Sunlight
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherGlyph(kind: WeatherKind, modifier: Modifier = Modifier, isDay: Boolean = true) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(28.dp)) { weatherDrawing(kind, isDay, color) }
}

private fun DrawScope.weatherDrawing(kind: WeatherKind, isDay: Boolean, color: Color) {
    val s = size.minDimension
    val stroke = Stroke(s * .055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val center = Offset(size.width / 2, size.height / 2)
    fun sun(at: Offset, r: Float) {
        drawCircle(if (isDay) Sunlight else color, r, at, style = stroke)
        if (isDay) repeat(8) { i ->
            val a = i * Math.PI / 4
            drawLine(Sunlight, at + Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * 1.5f), at + Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * 1.9f), s * .045f, StrokeCap.Round)
        } else drawCircle(color, r * .15f, at + Offset(r * .25f, -r * .3f))
    }
    fun cloud() {
        val p = Path().apply {
            moveTo(s * .26f, s * .67f)
            cubicTo(s * .03f, s * .67f, s * .08f, s * .38f, s * .30f, s * .43f)
            cubicTo(s * .35f, s * .16f, s * .72f, s * .20f, s * .73f, s * .43f)
            cubicTo(s * .98f, s * .39f, s * 1.01f, s * .67f, s * .78f, s * .67f)
            close()
        }
        drawPath(p, color.copy(alpha = .11f))
        drawPath(p, color, style = stroke)
    }
    when (kind) {
        WeatherKind.CLEAR -> sun(center, s * .2f)
        WeatherKind.CLOUD, WeatherKind.OVERCAST -> {
            if (kind == WeatherKind.CLOUD) sun(Offset(s * .69f, s * .29f), s * .12f)
            cloud()
        }
        WeatherKind.RAIN -> { cloud(); repeat(3) { i -> drawLine(color, Offset(s * (.3f + i * .2f), s * .79f), Offset(s * (.25f + i * .2f), s * .92f), s * .045f, StrokeCap.Round) } }
        WeatherKind.STORM -> {
            cloud()
            val p = Path().apply { moveTo(s * .53f, s * .66f); lineTo(s * .40f, s * .84f); lineTo(s * .57f, s * .84f); lineTo(s * .47f, s * .99f) }
            drawPath(p, Sunlight, style = stroke)
        }
        WeatherKind.SNOW -> { cloud(); repeat(3) { i -> drawCircle(color, s * .035f, Offset(s * (.28f + i * .24f), s * .84f)) } }
        WeatherKind.FOG -> repeat(3) { i -> drawLine(color, Offset(s * (.15f + (i % 2) * .1f), s * (.3f + i * .2f)), Offset(s * (.85f - (i % 2) * .1f), s * (.3f + i * .2f)), s * .05f, StrokeCap.Round) }
        WeatherKind.UNKNOWN -> drawCircle(color.copy(alpha = .5f), s * .22f, center, style = stroke)
    }
}

/** Original atmospheric artwork, drawn locally without image or animation dependencies. */
@Composable
fun WeatherArtwork(kind: WeatherKind, isDay: Boolean, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val storm = kind == WeatherKind.STORM
        val sun = Offset(w * .58f, h * .36f)
        val glow = if (isDay && kind != WeatherKind.SNOW) Sunlight else accent
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = if (storm) .08f else .18f), Color.Transparent), sun, w * .6f), w * .6f, sun)
        if (kind == WeatherKind.CLEAR || kind == WeatherKind.CLOUD) {
            drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = .95f), glow.copy(alpha = .6f)), sun, w * .19f), w * .19f, sun)
            drawCircle(glow.copy(alpha = .12f), w * .245f, sun, style = Stroke(1.dp.toPx()))
            drawCircle(glow.copy(alpha = .06f), w * .31f, sun, style = Stroke(1.dp.toPx()))
            if (!isDay) {
                drawCircle(Color(0xFF19252F), w * .17f, sun + Offset(w * .09f, -h * .05f))
                listOf(.18f to .2f, .85f to .16f, .3f to .06f).forEach { (x, y) -> drawCircle(accent.copy(alpha = .65f), 1.5.dp.toPx(), Offset(w * x, h * y)) }
            }
        }
        if (kind != WeatherKind.CLEAR && kind != WeatherKind.UNKNOWN) {
            val cloudColor = if (storm) Color(0xFF536778) else accent
            listOf(Triple(.08f, .49f, .75f), Triple(.25f, .40f, .65f), Triple(.15f, .56f, .78f)).forEachIndexed { i, (x, y, width) ->
                drawOval(Brush.verticalGradient(listOf(cloudColor.copy(alpha = .16f + i * .04f), cloudColor.copy(alpha = .02f)), h * y, h * (y + .26f)), Offset(w * x, h * y), Size(w * width, h * .28f))
            }
        }
        if (kind == WeatherKind.RAIN || storm) repeat(7) { i ->
            val x = w * (.2f + i * .09f)
            drawLine(accent.copy(alpha = .25f), Offset(x, h * .76f), Offset(x - w * .025f, h * .86f), 1.dp.toPx(), StrokeCap.Round)
        }
        if (kind == WeatherKind.SNOW) repeat(9) { i -> drawCircle(accent.copy(alpha = .45f), 1.8.dp.toPx(), Offset(w * (.18f + (i % 5) * .14f), h * (.77f + (i / 5) * .14f))) }
        if (kind == WeatherKind.FOG) repeat(4) { i -> drawLine(accent.copy(alpha = .13f), Offset(w * .08f, h * (.4f + i * .11f)), Offset(w * .92f, h * (.4f + i * .11f)), 10.dp.toPx(), StrokeCap.Round) }
        if (storm) {
            val p = Path().apply { moveTo(w * .57f, h * .57f); lineTo(w * .43f, h * .78f); lineTo(w * .55f, h * .78f); lineTo(w * .47f, h * .96f) }
            drawPath(p, Sunlight.copy(alpha = .8f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
