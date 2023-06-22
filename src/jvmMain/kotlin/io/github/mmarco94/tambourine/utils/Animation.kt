package io.github.mmarco94.tambourine.utils

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
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

// See AnimationModifier.kt (animateContentSize)
fun Modifier.animateContentHeight(
    animationSpec: FiniteAnimationSpec<Int> = spring(),
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animateContentHeight"
        properties["animationSpec"] = animationSpec
    }
) {
    val scope = rememberCoroutineScope()
    val animModifier = remember(scope) {
        HeightAnimationModifier(animationSpec, scope)
    }
    this.clipToBounds().then(animModifier)
}

/**
 * This class creates a [LayoutModifier] that measures children, and responds to children's size
 * change by animating to that size. The size reported to parents will be the animated size.
 */
private class HeightAnimationModifier(
    val animSpec: AnimationSpec<Int>,
    val scope: CoroutineScope,
) : LayoutModifier {
    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)

    data class AnimData(
        val anim: Animatable<Int, AnimationVector1D>,
        var startSize: Int
    )

    var animData: AnimData? by mutableStateOf(null)

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val placeable = measurable.measure(constraints)

        val measuredSize = IntSize(placeable.width, placeable.height)

        val height = animateTo(measuredSize.height)
        return layout(measuredSize.width, height) {
            placeable.placeRelative(0, 0)
        }
    }

    fun animateTo(targetSize: Int): Int {
        val data = animData?.apply {
            if (targetSize != anim.targetValue) {
                startSize = anim.value
                scope.launch {
                    anim.animateTo(targetSize, animSpec)
                }
            }
        } ?: AnimData(
            Animatable(
                targetSize, Int.VectorConverter, visibilityThreshold = 1
            ),
            targetSize
        )

        animData = data
        return data.anim.value
    }
}