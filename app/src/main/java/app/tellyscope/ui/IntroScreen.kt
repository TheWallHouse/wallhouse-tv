// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.tellyscope.R
import kotlin.math.min

/**
 * The opening scene, once per launch, right after Android's plain black splash: the lit edge of a planet, a glow on
 * the horizon and the icon's trajectory lifting off from it, then the TV and WALLHOUSE TV. The app loads underneath
 * meanwhile. With animations off on the phone it shows the last frame briefly instead.
 */
object Intro {
    /** Shown once per process, so coming back to the app skips it. */
    @Volatile var shown = false

    const val TOTAL_MS = 1350
    const val STILL_MS = 400
}

private val LIFT = CubicBezierEasing(.3f, .6f, .2f, 1f)

/** [t] in ms, as the 0..1 progress of the step from [start] to [end], eased by [easing]. */
private fun step(t: Float, start: Int, end: Int, easing: (Float) -> Float = { it }) =
    easing(((t - start) / (end - start)).coerceIn(0f, 1f))

@Composable
fun IntroScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val still = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val time = remember { Animatable(if (still) Intro.TOTAL_MS.toFloat() else 0f) }
    val out = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (still) {
            kotlinx.coroutines.delay(Intro.STILL_MS.toLong())
        } else {
            time.animateTo(Intro.TOTAL_MS.toFloat(), tween(Intro.TOTAL_MS, easing = LinearEasing))
            out.animateTo(0f, tween(220))
        }
        onDone()
    }
    val t = time.value
    Box(Modifier.fillMaxSize().alpha(out.value).background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            stars()
            planet()
            val icon = min(size.width * .46f, size.height * .3f)
            val origin = Offset((size.width - icon) / 2, size.height * .44f - icon / 2)
            launch(origin, icon, t)
        }
        Image(
            painterResource(R.drawable.splash_branding),
            contentDescription = "WallHouse TV",
            colorFilter = ColorFilter.tint(Color(0xFFF2F4F5)),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp)
                .size(200.dp, 80.dp).alpha(step(t, 950, 1350)),
        )
    }
}

/** A few fixed stars, like the mock-up's. */
private val STARS = listOf(
    Triple(.14f, .18f, .55f), Triple(.72f, .12f, .4f), Triple(.38f, .32f, .35f), Triple(.86f, .40f, .45f),
    Triple(.24f, .56f, .3f), Triple(.60f, .48f, .35f), Triple(.50f, .22f, .6f), Triple(.08f, .70f, .3f),
    Triple(.92f, .64f, .3f), Triple(.30f, .08f, .4f),
)

private fun DrawScope.stars() {
    val r = 1.dp.toPx()
    for ((x, y, a) in STARS) drawCircle(Color.White.copy(alpha = a), r, Offset(size.width * x, size.height * y))
}

/** The planet: a disc much wider than the screen, its top edge lit at 76% of the height. */
private fun DrawScope.planet() {
    val r = size.width * .8f
    val center = Offset(size.width / 2, size.height * .76f + r)
    val outer = r * 1.1f
    val edge = r / outer
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFF0B1014),
            edge * .97f to Color(0xFF121A20),
            edge to Color(0xFFDFE7EC),
            edge + .012f to Color(0x8C7FA2BE),
            edge + .04f to Color(0x2E3C648C),
            1f to Color.Transparent,
            center = center,
            radius = outer,
        ),
        radius = outer,
        center = center,
    )
}

/** The icon's art (108 × 108, scaled 68% about its centre as on the launcher icon) at [origin], [icon] wide, at [t] ms. */
private fun DrawScope.launch(origin: Offset, icon: Float, t: Float) {
    fun p(x: Float, y: Float) = Offset(origin.x + (54 + (x - 54) * .68f) / 108f * icon, origin.y + (54 + (y - 54) * .68f) / 108f * icon)
    val unit = .68f / 108f * icon
    // The glow on the horizon where the trajectory starts: a soft streak, no edge, that opens and fades.
    val glow = step(t, 50, 650)
    val glowAlpha = if (glow < .3f) glow / .3f else 1f - (glow - .3f) / .7f
    if (glow > 0f && glow < 1f) {
        val c = p(20f, 84f)
        val stretch = .4f + 1.2f * glow
        withTransform({ scale(5.5f * stretch, 1f, c) }) {
            drawCircle(
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = .95f * glowAlpha),
                    .35f to Color(0xFFD8E6F0).copy(alpha = .45f * glowAlpha),
                    1f to Color.Transparent,
                    center = c,
                    radius = 4 * unit,
                ),
                radius = 4 * unit,
                center = c,
            )
        }
    }
    // The TV behind, once the trajectory is up.
    val tv = step(t, 700, 1050)
    if (tv > 0f) {
        val stroke = Stroke(3.4f * unit)
        val tl = p(31f, 39f)
        val br = p(77f, 69f)
        drawRoundRect(
            Color.White.copy(alpha = tv), tl, Size(br.x - tl.x, br.y - tl.y),
            androidx.compose.ui.geometry.CornerRadius(2.5f * unit), style = stroke,
        )
        drawLine(Color.White.copy(alpha = tv), p(46f, 74.5f), p(62f, 74.5f), 3.4f * unit, StrokeCap.Round)
    }
    // The trajectory, drawn from the horizon up.
    val lift = step(t, 100, 800, LIFT::transform)
    if (lift > 0f) {
        val a = p(20f, 84f)
        val full = Path().apply {
            moveTo(a.x, a.y)
            val c1 = p(38f, 62f)
            val c2 = p(60f, 42f)
            val e = p(92f, 25f)
            cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
        }
        val measure = PathMeasure().apply { setPath(full, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * lift, part, true)
        drawPath(
            part,
            Brush.linearGradient(
                0f to Color.White.copy(alpha = 0f), .55f to Color.White.copy(alpha = .9f), 1f to Color.White,
                start = a, end = p(92f, 25f),
            ),
            style = Stroke(4f * unit, cap = StrokeCap.Round),
        )
    }
}
