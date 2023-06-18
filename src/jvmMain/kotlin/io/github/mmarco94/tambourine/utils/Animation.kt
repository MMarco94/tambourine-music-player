package io.github.mmarco94.tambourine.utils

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch


@Composable
fun animateOrSnapFloatAsState(
    targetValue: Pair<Boolean, Float>,
    visibilityThreshold: Float = 0.01f,
    label: String = "FloatAnimation",
): State<Float> {
    val resolvedAnimSpec = remember(visibilityThreshold) { spring(visibilityThreshold = visibilityThreshold) }
    return animateOrSnapValueAsState(
        targetValue,
        Float.VectorConverter,
        resolvedAnimSpec,
        visibilityThreshold,
        label,
    )
}

@Composable
fun <T, V : AnimationVector> animateOrSnapValueAsState(
    targetValue: Pair<Boolean, T>,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: AnimationSpec<T> = remember { spring() },
    visibilityThreshold: T? = null,
    label: String = "ValueAnimation",
): State<T> {
    //See animateValueAsState
    val toolingOverride = remember { mutableStateOf<State<T>?>(null) }
    val animatable = remember { Animatable(targetValue.second, typeConverter, visibilityThreshold, label) }
    val animSpec: AnimationSpec<T> by rememberUpdatedState(
        animationSpec.run {
            if (visibilityThreshold != null && this is SpringSpec &&
                this.visibilityThreshold != visibilityThreshold
            ) {
                spring(dampingRatio, stiffness, visibilityThreshold)
            } else {
                this
            }
        }
    )
    val channel = remember { Channel<Pair<Boolean, T>>(Channel.CONFLATED) }
    SideEffect {
        channel.trySend(targetValue)
    }
    LaunchedEffect(channel) {
        for (target in channel) {
            // This additional poll is needed because when the channel suspends on receive and
            // two values are produced before consumers' dispatcher resumes, only the first value
            // will be received.
            // It may not be an issue elsewhere, but in animation we want to avoid being one
            // frame late.
            val (snap, newTarget) = channel.tryReceive().getOrNull() ?: target
            launch {
                if (newTarget != animatable.targetValue) {
                    if (snap) {
                        animatable.snapTo(newTarget)
                    } else {
                        animatable.animateTo(newTarget, animSpec)
                    }
                }
            }
        }
    }
    return toolingOverride.value ?: animatable.asState()
}
