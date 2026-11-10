package ru.muz.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable

internal val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
internal val EmphasizedExit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

internal val PressSpring: AnimationSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 700f)

@Composable
internal fun revealIn(): EnterTransition {
    if (LocalReduceMotion.current) return EnterTransition.None
    return fadeIn(tween(animationDuration(220), delayMillis = animationDuration(40))) +
        expandVertically(tween(animationDuration(280), easing = EmphasizedEasing), expandFrom = androidx.compose.ui.Alignment.Top)
}

@Composable
internal fun revealOut(): ExitTransition {
    if (LocalReduceMotion.current) return ExitTransition.None
    return fadeOut(tween(animationDuration(140))) +
        shrinkVertically(tween(animationDuration(240), easing = EmphasizedEasing), shrinkTowards = androidx.compose.ui.Alignment.Top)
}

@Composable
internal fun pageEnter(forward: Boolean): EnterTransition {
    if (LocalReduceMotion.current) return fadeIn(tween(1))
    val duration = animationDuration(300)
    return fadeIn(tween(duration / 2, delayMillis = duration / 6)) +
        slideInHorizontally(tween(duration, easing = EmphasizedEasing)) { width -> (if (forward) width else -width) / 6 }
}

@Composable
internal fun pageExit(forward: Boolean): ExitTransition {
    if (LocalReduceMotion.current) return fadeOut(tween(1))
    val duration = animationDuration(220)
    return fadeOut(tween(duration / 2)) +
        slideOutHorizontally(tween(duration, easing = EmphasizedExit)) { width -> (if (forward) -width else width) / 10 }
}
