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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.animation.core.animateFloatAsState
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
internal fun AriaTabBar(
    tabs: List<MainTab>,
    selected: MainTab,
    onTab: (MainTab) -> Unit,
    showLabels: Boolean,
    backdrop: GlassBackdrop?,
    haptics: Boolean,
    insets: Boolean = true,
) {
    val style = LocalAppStyle.current
    val glass = style == AppStyle.GLASS
    val reduce = LocalReduceMotion.current
    val index = tabs.indexOf(selected).coerceAtLeast(0)
    val lead = remember { Animatable(index.toFloat()) }
    val trail = remember { Animatable(index.toFloat()) }
    var drag by remember { mutableStateOf<Float?>(null) }
    val press by animateFloatAsState(if (drag != null) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "tab-press")
    LaunchedEffect(index, drag) {
        val target = drag ?: index.toFloat()
        if (drag != null || reduce) {
            lead.snapTo(target)
            trail.snapTo(target)
            return@LaunchedEffect
        }
        val forward = target >= lead.value
        val front = if (forward) lead else trail
        val back = if (forward) trail else lead
        launch { front.animateTo(target, spring(dampingRatio = 0.82f, stiffness = 520f)) }
        launch { back.animateTo(target, spring(dampingRatio = 0.86f, stiffness = 170f)) }
    }
    val haptic = LocalHapticFeedback.current
    val currentSelected by androidx.compose.runtime.rememberUpdatedState(selected)
    val select by androidx.compose.runtime.rememberUpdatedState(onTab)
    val scheme = MaterialTheme.colorScheme
    val barHeight = if (glass) 64.dp else if (showLabels) 72.dp else 60.dp
    val glassShape = RoundedCornerShape(50)
    val outer = if (glass) {
        Modifier.then(if (insets) Modifier.navigationBarsPadding() else Modifier)
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp, top = 4.dp)
            .fillMaxWidth().height(barHeight)
            .graphicsLayer {
                val k = 1f + 0.025f * press
                scaleX = k
                scaleY = k
            }
            .glass(backdrop, glassShape, pressed = press)
    } else {
        Modifier.fillMaxWidth()
            .background(if (style == AppStyle.MINIMAL) scheme.background else scheme.surfaceContainer)
            .then(
                if (style == AppStyle.MINIMAL) Modifier.drawWithContent {
                    drawContent()
                    drawLine(scheme.onSurface.copy(alpha = 0.12f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                } else Modifier,
            )
            .then(if (insets) Modifier.navigationBarsPadding() else Modifier)
            .height(barHeight)
    }
    BoxWithConstraints(outer) {
        val count = tabs.size.coerceAtLeast(1)
        val tabWidth = maxWidth / count
        val density = androidx.compose.ui.platform.LocalDensity.current
        val tabPx = with(density) { tabWidth.toPx() }
        val from = minOf(lead.value, trail.value)
        val to = maxOf(lead.value, trail.value)
        val indicatorColor = when (style) {
            AppStyle.GLASS -> scheme.onSurface.copy(alpha = 0.09f + 0.05f * press)
            AppStyle.MINIMAL -> scheme.onSurface.copy(alpha = 0.08f)
            AppStyle.MATERIAL_YOU -> scheme.primaryContainer
            AppStyle.CLASSIC -> scheme.secondaryContainer
        }
        if (glass) {
            val inset = 5.dp
            Box(
                Modifier
                    .offset(x = tabWidth * from + inset)
                    .padding(vertical = inset)
                    .width(tabWidth * (to - from + 1f) - inset * 2)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val k = 1f + 0.08f * press
                        scaleX = k
                        scaleY = k
                    }
                    .clip(RoundedCornerShape(50))
                    .background(indicatorColor)
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f + 0.25f * press), Color.White.copy(alpha = 0.05f))),
                        RoundedCornerShape(50),
                    ),
            )
        } else {
            val pillWidth = if (style == AppStyle.MATERIAL_YOU) 64.dp else 56.dp
            val pillHeight = 32.dp
            Box(
                Modifier
                    .offset(x = tabWidth * from + (tabWidth - pillWidth) / 2, y = if (showLabels) 10.dp else (barHeight - pillHeight) / 2)
                    .size(tabWidth * (to - from) + pillWidth, pillHeight)
                    .clip(if (style == AppStyle.MINIMAL) RoundedCornerShape(8.dp) else RoundedCornerShape(50))
                    .background(indicatorColor),
            )
        }
        Row(
            Modifier.fillMaxSize().pointerInput(tabs, tabPx) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startX = down.position.x
                    var moved = false
                    var last = startX
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        last = change.position.x
                        if (!moved && abs(last - startX) > viewConfiguration.touchSlop) moved = true
                        if (moved) {
                            val position = (last / tabPx - 0.5f).coerceIn(0f, (count - 1).toFloat())
                            val before = drag?.let { kotlin.math.round(it).toInt() }
                            drag = position
                            val now = kotlin.math.round(position).toInt()
                            if (haptics && before != null && before != now) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            change.consume()
                        }
                    }
                    val target = (last / tabPx).toInt().coerceIn(0, count - 1)
                    drag = null
                    val tab = tabs[target]
                    if (tab != currentSelected && haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    select(tab)
                }
            },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, item ->
                val focus = drag?.let { (1f - abs(it - i)).coerceIn(0f, 1f) }
                val active = if (focus != null) focus > 0.5f else item == selected
                val chosen = item == selected
                TabItem(item, active, showLabels, glass, Modifier.weight(1f).fillMaxHeight().semantics {
                    role = Role.Tab
                    this.selected = chosen
                    contentDescription = item.title
                    onClick(label = item.title) { onTab(item); true }
                })
            }
        }
    }
}

@Composable
private fun TabItem(item: MainTab, active: Boolean, label: Boolean, glass: Boolean, modifier: Modifier) {
    val style = LocalAppStyle.current
    val scheme = MaterialTheme.colorScheme
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (active && !reduce) {
            pop.snapTo(0.8f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val tint by animateColorAsState(
        when {
            active && glass -> Accent
            active && style == AppStyle.MATERIAL_YOU -> scheme.onPrimaryContainer
            active && style == AppStyle.CLASSIC -> scheme.onSecondaryContainer
            active -> scheme.onSurface
            else -> scheme.onSurface.copy(alpha = if (glass) 0.78f else 0.62f)
        },
        tween(animationDuration(200)),
        label = "tab-tint",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(
            if (glass) GlassIcons.forTab(item, active) else if (active) item.active else item.inactive,
            null,
            tint = tint,
            modifier = Modifier.size(if (glass) 25.dp else 24.dp).graphicsLayer {
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
                modifier = Modifier.padding(top = if (glass) 2.dp else 6.dp),
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
        seekStyle = ru.muz.settings.SeekStyle.WAVY,
        accentControls = true,
    )
    AppStyle.GLASS -> settings.copy(
        appStyle = style,
        useDynamicColors = false,
        accentStyle = ru.muz.settings.AccentStyle.BLUE,
        playShape = ru.muz.settings.PlayShape.CIRCLE,
        seekStyle = ru.muz.settings.SeekStyle.CAPSULE,
        coverAccent = true,
        pulseBackground = true,
    )
    AppStyle.MINIMAL -> settings.copy(
        appStyle = style,
        useDynamicColors = false,
        coverAccent = false,
        accentControls = false,
        playShape = ru.muz.settings.PlayShape.SQUARE,
        seekStyle = ru.muz.settings.SeekStyle.HAIRLINE,
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

@Composable
internal fun Modifier.pressScale(amount: Float = 0.96f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed && !LocalReduceMotion.current) amount else 1f,
        spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "press-scale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

