package ru.muz.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.abs
import ru.muz.settings.AppStyle

val LocalAppStyle = staticCompositionLocalOf { AppStyle.CLASSIC }
val LocalGlassStrength = staticCompositionLocalOf { 0.6f }

internal object CookieShape : Shape {
    private val polygon = RoundedPolygon.star(
        numVerticesPerRadius = 9,
        innerRadius = 0.82f,
        rounding = CornerRounding(0.32f),
        innerRounding = CornerRounding(0.32f),
    )

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = polygon.toPath().asComposePath()
        val matrix = Matrix()
        matrix.translate(size.width / 2f, size.height / 2f)
        matrix.scale(size.width / 2f, size.height / 2f)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

@Composable
internal fun Modifier.glass(haze: HazeState?, shape: Shape, strength: Float = LocalGlassStrength.current): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminance() < 0.4f
    val tint = if (dark) Color(0xFF1C1C20) else Color.White
    val edge = if (dark) Color.White else Color.Black
    val blurred = haze != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return this
        .shadow(18.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
        .clip(shape)
        .then(
            if (blurred) {
                Modifier.hazeEffect(haze) {
                    backgroundColor = surface
                    blurRadius = (14f + 22f * strength).dp
                    tints = listOf(HazeTint(tint.copy(alpha = (if (dark) 0.50f else 0.58f) - 0.25f * strength)))
                    noiseFactor = 0.04f
                }
            } else {
                Modifier.background(tint.copy(alpha = 0.92f))
            },
        )
        .drawWithContent {
            drawContent()
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (dark) 0.10f else 0.35f), Color.Transparent),
                    endY = size.height * 0.55f,
                ),
            )
        }
        .border(
            1.dp,
            Brush.verticalGradient(listOf(edge.copy(alpha = if (dark) 0.28f else 0.16f), edge.copy(alpha = if (dark) 0.06f else 0.05f))),
            shape,
        )
}

@Composable
internal fun AriaTabBar(
    tabs: List<MainTab>,
    selected: MainTab,
    onTab: (MainTab) -> Unit,
    showLabels: Boolean,
    haze: HazeState?,
    haptics: Boolean,
) {
    val style = LocalAppStyle.current
    val glass = style == AppStyle.GLASS
    val reduce = LocalReduceMotion.current
    val index = tabs.indexOf(selected).coerceAtLeast(0)
    val position = remember { Animatable(index.toFloat()) }
    LaunchedEffect(index) {
        if (reduce) position.snapTo(index.toFloat())
        else position.animateTo(index.toFloat(), spring(dampingRatio = 0.68f, stiffness = 380f))
    }
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val barHeight = if (glass) 62.dp else if (showLabels) 72.dp else 60.dp
    val outer = if (glass) {
        Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 10.dp, top = 4.dp)
            .fillMaxWidth().height(barHeight).glass(haze, RoundedCornerShape(50))
    } else {
        Modifier.fillMaxWidth()
            .background(
                when (style) {
                    AppStyle.MINIMAL -> scheme.background
                    AppStyle.MATERIAL_YOU -> scheme.surfaceContainer
                    else -> scheme.surfaceContainer
                },
            )
            .then(
                if (style == AppStyle.MINIMAL) Modifier.drawWithContent {
                    drawContent()
                    drawLine(scheme.onSurface.copy(alpha = 0.12f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                } else Modifier,
            )
            .navigationBarsPadding().height(barHeight)
    }
    BoxWithConstraints(outer) {
        val count = tabs.size.coerceAtLeast(1)
        val tabWidth = maxWidth / count
        val stretch = if (reduce) 1f else 1f + (abs(position.velocity) / 10f).coerceAtMost(0.45f)
        val indicatorColor = when (style) {
            AppStyle.GLASS -> scheme.onSurface.copy(alpha = 0.10f)
            AppStyle.MINIMAL -> scheme.onSurface.copy(alpha = 0.08f)
            AppStyle.MATERIAL_YOU -> scheme.primaryContainer
            AppStyle.CLASSIC -> scheme.secondaryContainer
        }
        if (glass) {
            val inset = 5.dp
            val width = tabWidth - inset * 2
            Box(
                Modifier
                    .offset(x = tabWidth * position.value + inset)
                    .padding(vertical = inset)
                    .width(width)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = stretch
                        scaleY = 1f - (stretch - 1f) * 0.25f
                    }
                    .clip(RoundedCornerShape(50))
                    .background(indicatorColor)
                    .border(1.dp, scheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(50)),
            )
        } else {
            val pillWidth = if (style == AppStyle.MATERIAL_YOU) 64.dp else 56.dp
            val pillHeight = 32.dp
            Box(
                Modifier
                    .offset(x = tabWidth * position.value + (tabWidth - pillWidth) / 2, y = if (showLabels) 10.dp else (barHeight - pillHeight) / 2)
                    .size(pillWidth, pillHeight)
                    .graphicsLayer { scaleX = stretch }
                    .clip(if (style == AppStyle.MINIMAL) RoundedCornerShape(8.dp) else RoundedCornerShape(50))
                    .background(indicatorColor),
            )
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { item ->
                val active = item == selected
                TabItem(item, active, showLabels, glass, Modifier.weight(1f).fillMaxHeight()) {
                    if (item != selected && haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTab(item)
                }
            }
        }
    }
}

@Composable
private fun TabItem(item: MainTab, active: Boolean, label: Boolean, glass: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val style = LocalAppStyle.current
    val scheme = MaterialTheme.colorScheme
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (active && !reduce) {
            pop.snapTo(0.78f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val tint by animateColorAsState(
        when {
            active && style == AppStyle.MATERIAL_YOU -> scheme.onPrimaryContainer
            active && style == AppStyle.CLASSIC -> scheme.onSecondaryContainer
            active -> scheme.onSurface
            else -> scheme.onSurface.copy(alpha = 0.62f)
        },
        tween(animationDuration(220)),
        label = "tab-tint",
    )
    Column(
        modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (active) item.active else item.inactive,
            item.title,
            tint = tint,
            modifier = Modifier.size(24.dp).graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
        )
        if (label) {
            Text(
                item.title,
                color = tint,
                fontSize = if (glass) 10.sp else 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(top = if (glass) 1.dp else 6.dp),
            )
        }
    }
}

internal fun stylePreset(settings: ru.muz.settings.AppSettings, style: AppStyle): ru.muz.settings.AppSettings = when (style) {
    AppStyle.CLASSIC -> settings.copy(appStyle = style)
    AppStyle.MATERIAL_YOU -> settings.copy(
        appStyle = style,
        useDynamicColors = true,
        playShape = ru.muz.settings.PlayShape.COOKIE,
        wavySeekBar = true,
        accentControls = true,
    )
    AppStyle.GLASS -> settings.copy(
        appStyle = style,
        playShape = ru.muz.settings.PlayShape.CIRCLE,
        coverAccent = true,
        pulseBackground = true,
    )
    AppStyle.MINIMAL -> settings.copy(
        appStyle = style,
        useDynamicColors = false,
        coverAccent = false,
        accentControls = false,
        playShape = ru.muz.settings.PlayShape.SQUARE,
        wavySeekBar = false,
        coverSparks = false,
    )
}

internal fun styleName(style: AppStyle): String = when (style) {
    AppStyle.CLASSIC -> "Классика"
    AppStyle.MATERIAL_YOU -> "Material You"
    AppStyle.GLASS -> "Стекло"
    AppStyle.MINIMAL -> "Минимал"
}

internal fun styleHint(style: AppStyle): String = when (style) {
    AppStyle.CLASSIC -> "Привычный вид Aria"
    AppStyle.MATERIAL_YOU -> "Цвета от обоев, мягкие формы"
    AppStyle.GLASS -> "Размытые панели поверх контента"
    AppStyle.MINIMAL -> "Чёрно-белый, без лишнего"
}
