package ru.muz.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalPlayerSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
private val LocalPlayerVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PlayerScene(expanded: Boolean, content: @Composable (Boolean) -> Unit) {
    val duration = playerTransitionDuration(480)
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        val shared = this
        AnimatedContent(
            targetState = expanded,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(duration, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(duration / 2))
            },
            label = "player-scene",
        ) { visible ->
            CompositionLocalProvider(
                LocalPlayerSharedScope provides shared,
                LocalPlayerVisibilityScope provides this,
            ) { content(visible) }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.playerArtworkTransition(mediaId: String, coverUrl: String?): Modifier {
    val shared = LocalPlayerSharedScope.current ?: return this
    val visibility = LocalPlayerVisibilityScope.current ?: return this
    if (!LocalPlayerTransitions.current || LocalReduceMotion.current || mediaId.isBlank() || coverUrl.isNullOrBlank()) return this
    val loader = coil.Coil.imageLoader(androidx.compose.ui.platform.LocalContext.current)
    if (loader.memoryCache?.get(coil.memory.MemoryCache.Key(coverUrl)) == null) return this
    val duration = playerTransitionDuration(480)
    return with(shared) {
        sharedElement(
            rememberSharedContentState("player-artwork:$mediaId"),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> tween(duration, easing = FastOutSlowInEasing) },
        )
    }
}
