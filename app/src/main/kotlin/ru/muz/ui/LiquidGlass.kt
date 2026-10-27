package ru.muz.ui

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Stable
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var version by mutableIntStateOf(0)
    internal var coordinates: androidx.compose.ui.layout.LayoutCoordinates? = null
}

@Composable
internal fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

internal fun Modifier.glassSource(backdrop: GlassBackdrop?): Modifier {
    if (backdrop == null) return this
    return onGloballyPositioned { backdrop.coordinates = it }
        .drawWithContent {
            backdrop.layer.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.layer)
            Snapshot.withoutReadObservation { backdrop.version += 1 }
        }
}

val LocalGlassLight = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

@Composable
internal fun rememberGlassLight(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val light = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled, owner) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (!enabled || manager == null || sensor == null) {
            light.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val target = Offset((-event.values[0] / 9.81f).coerceIn(-1f, 1f), (event.values[1] / 9.81f - 0.6f).coerceIn(-1f, 1f))
                val current = light.value
                val next = current + (target - current) * 0.12f
                if ((next - current).getDistance() > 0.004f) light.value = next
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        var registered = false
        fun start() {
            if (!registered) registered = manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        fun stop() {
            if (registered) manager.unregisterListener(listener)
            registered = false
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> start()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) start()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            stop()
        }
    }
    return light
}

private const val LENS = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float band;
uniform float bend;
uniform float chroma;

float sdf(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - float2(radius));
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

half4 main(float2 p) {
    float d = sdf(p);
    float k = 1.0 - clamp(-d / band, 0.0, 1.0);
    float2 g = float2(sdf(p + float2(1.0, 0.0)) - sdf(p - float2(1.0, 0.0)), sdf(p + float2(0.0, 1.0)) - sdf(p - float2(0.0, 1.0)));
    float gl = length(g);
    float2 n = gl > 0.0001 ? g / gl : float2(0.0);
    float amount = bend * k * k * k;
    float2 base = p - n * amount;
    float split = chroma * k * k;
    half4 mid = content.eval(base);
    half r = content.eval(base - n * split).r;
    half b = content.eval(base + n * split).b;
    return half4(r, mid.g, b, mid.a);
}
"""

@Composable
internal fun Modifier.glass(
    backdrop: GlassBackdrop?,
    shape: Shape,
    strength: Float = LocalGlassStrength.current,
    base: Color = AppBackground,
    pressed: Float = 0f,
    corner: androidx.compose.ui.unit.Dp? = null,
): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminance() < 0.4f
    val tint = if (dark) Color(0xFF16161A) else Color.White
    val light by LocalGlassLight.current
    var relative by remember { mutableStateOf(Offset.Zero) }
    val sdk = Build.VERSION.SDK_INT
    val live = backdrop != null && sdk >= Build.VERSION_CODES.S
    return this
        .shadow(if (dark) 22.dp else 16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.28f))
        .onGloballyPositioned { coords ->
            val source = backdrop?.coordinates
            relative = if (source != null && source.isAttached && coords.isAttached) source.localPositionOf(coords, Offset.Zero) else Offset.Zero
        }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            val radius = (corner?.toPx() ?: (minOf(size.width, size.height) / 2f)).coerceAtMost(minOf(size.width, size.height) / 2f)
            val layer = if (live) obtainGraphicsLayer() else null
            if (layer != null) {
                val blur = (1.5f + 5f * strength).dp.toPx()
                val saturate = ColorMatrix().apply { setSaturation(1.45f) }
                val blurred = RenderEffect.createColorFilterEffect(
                    ColorMatrixColorFilter(saturate),
                    RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP),
                )
                layer.renderEffect = if (sdk >= Build.VERSION_CODES.TIRAMISU) {
                    val shader = RuntimeShader(LENS)
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setFloatUniform("radius", radius)
                    shader.setFloatUniform("band", (14f + 12f * strength).dp.toPx().coerceAtMost(size.height / 2f))
                    shader.setFloatUniform("bend", (9f + 15f * strength).dp.toPx())
                    shader.setFloatUniform("chroma", (0.6f + 1.4f * strength).dp.toPx())
                    RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"), blurred).asComposeRenderEffect()
                } else {
                    RenderEffect.createChainEffect(
                        RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(saturate)),
                        RenderEffect.createBlurEffect((10f + 14f * strength).dp.toPx(), (10f + 14f * strength).dp.toPx(), Shader.TileMode.CLAMP),
                    ).asComposeRenderEffect()
                }
            }
            onDrawWithContent {
                clipPath(path) {
                    if (layer != null && backdrop != null) {
                        backdrop.version
                        drawRect(base)
                        val dx = -relative.x
                        val dy = -relative.y
                        layer.record { translate(dx, dy) { drawLayer(backdrop.layer) } }
                        drawLayer(layer)
                        drawRect(tint.copy(alpha = (if (dark) 0.12f else 0.18f) + 0.16f * strength + 0.06f * pressed))
                    } else {
                        drawRect(tint.copy(alpha = 0.93f))
                    }
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = if (dark) 0.07f else 0.22f), Color.Transparent),
                            endY = size.height * 0.5f,
                        ),
                    )
                }
                val start = Offset(size.width * (0.18f - 0.35f * light.x), size.height * (0f - 0.4f * light.y))
                val end = Offset(size.width * (0.82f - 0.35f * light.x), size.height * (1f - 0.4f * light.y))
                val rim = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.55f else 0.85f),
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.03f),
                        Color.White.copy(alpha = if (dark) 0.30f else 0.5f),
                    ),
                    start = start,
                    end = end,
                )
                clipPath(path) {
                    drawPath(
                        path,
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.White.copy(alpha = 0.08f)), start = start, end = end),
                        style = Stroke(8.dp.toPx()),
                    )
                }
                drawPath(path, rim, style = Stroke((1.1f + 0.6f * pressed).dp.toPx()))
                if (!dark) drawPath(path, Color.Black.copy(alpha = 0.06f), style = Stroke(0.6.dp.toPx()))
                drawContent()
            }
        }
}
