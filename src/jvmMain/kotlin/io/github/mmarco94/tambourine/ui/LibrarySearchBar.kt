package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.*
import io.github.mmarco94.tambourine.data.Library
import io.github.mmarco94.tambourine.ui.LibrarySearchBarMode.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class LibrarySearchBarMode {
    EXPANDED, ICON, TAG;

    @Composable
    fun textStyle(): TextStyle {
        return if (this == TAG) {
            MaterialTheme.typography.labelLarge
        } else {
            LocalTextStyle.current
        }
    }
}

private val floatTransitionSpec: @Composable Transition.Segment<LibrarySearchBarMode>.() -> FiniteAnimationSpec<Float> =
    {
        if (this.targetState == this.initialState) snap() else spring()
    }
private val offsetTransitionSpec: @Composable Transition.Segment<LibrarySearchBarMode>.() -> FiniteAnimationSpec<Offset> =
    {
        if (this.targetState == this.initialState) snap() else spring(visibilityThreshold = Offset.VisibilityThreshold)
    }
private val sizeTransitionSpec: @Composable Transition.Segment<LibrarySearchBarMode>.() -> FiniteAnimationSpec<Size> = {
    if (this.targetState == this.initialState) snap() else spring(visibilityThreshold = Size.VisibilityThreshold)
}
private val dpTransitionSpec: @Composable Transition.Segment<LibrarySearchBarMode>.() -> FiniteAnimationSpec<Dp> = {
    if (this.targetState == this.initialState) snap() else spring(visibilityThreshold = Dp.VisibilityThreshold)
}

@Composable
fun BoxScope.LibrarySearchBar(
    mode: Transition<LibrarySearchBarMode>,
    collapse: () -> Unit,
    iconOffset: Offset,
    tagOffset: Offset,
    tagSize: IntSize,
    expandedSize: IntSize,
    library: Library,
    query: String,
    setQuery: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var placeholder by remember { mutableStateOf("") }
    LaunchedEffect(library) {
        while (true) {
            placeholder = library.artists.randomOrNull()?.name.orEmpty()
            delay(5000)
            placeholder = library.albums.randomOrNull()?.title.orEmpty()
            delay(5000)
        }
    }
    val bgAlpha by mode.animateFloat(floatTransitionSpec) { if (it == ICON) 0f else 1f }
    val closeAlpha by mode.animateFloat(floatTransitionSpec) { if (it == ICON) 0f else 1f }
    val realTextFieldAlpha by mode.animateFloat(floatTransitionSpec) { if (it == EXPANDED) 1f else 0f }
    val offset by mode.animateOffset(offsetTransitionSpec) {
        when (it) {
            EXPANDED -> Offset.Zero
            ICON -> iconOffset
            TAG -> tagOffset
        }
    }
    val size by mode.animateSize(sizeTransitionSpec) {
        when (it) {
            EXPANDED -> expandedSize.toSize()
            ICON -> Size(48.dp.toPxApprox(), 48.dp.toPxApprox())
            TAG -> tagSize.toSize()
        }
    }
    val textSize by mode.animateFloat(floatTransitionSpec) {
        it.textStyle().fontSize.value
    }
    val fontWeight by mode.animateFloat(floatTransitionSpec) {
        it.textStyle().fontWeight?.weight?.toFloat() ?: 0f
    }
    val lineHeight by mode.animateFloat(floatTransitionSpec) {
        it.textStyle().lineHeight.value
    }
    val letterSpacing by mode.animateFloat(floatTransitionSpec) {
        it.textStyle().letterSpacing.value
    }
    val iconPadding by mode.animateDp(dpTransitionSpec) {
        if (it == EXPANDED) 8.dp else 0.dp
    }
    val textPadding by mode.animateDp(dpTransitionSpec) {
        if (it == EXPANDED) 12.dp else 0.dp
    }
    if (mode.targetState == EXPANDED || mode.currentState == EXPANDED) {
        var textValue by remember { mutableStateOf(TextFieldValue(query, selection = TextRange(0, query.length))) }
        Box(
            Modifier.matchParentSize()
        ) {
            Box(Modifier
                .offset { offset.round() }
                .size(size.toDpApprox())
                .padding(2.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = bgAlpha)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val textStyle = LocalTextStyle.current.copy(
                            fontSize = textSize.sp,
                            fontWeight = FontWeight(fontWeight.roundToInt()),
                            lineHeight = lineHeight.sp,
                            letterSpacing = letterSpacing.sp,
                        )
                        SingleLineText(
                            query,
                            Modifier.fillMaxWidth()
                                .alpha(1 - realTextFieldAlpha)
                                .padding(horizontal = 40.dp)
                                .padding(start = textPadding),
                            style = textStyle
                        )
                        TextField(
                            textValue.copy(text = query),
                            { textValue = it; setQuery(it.text) },
                            modifier = Modifier
                                .alpha(realTextFieldAlpha)
                                .focusRequester(focusRequester)
                                .fillMaxSize()
                                .onKeyEvent {
                                    if (it.key == Key.Enter) {
                                        collapse()
                                        true
                                    } else false
                                },
                            leadingIcon = { },
                            trailingIcon = { },
                            placeholder = { Crossfade(placeholder) { Text(it) } },
                            shape = RectangleShape,
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            ),
                            keyboardActions = KeyboardActions { collapse() },
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.padding(8.dp).padding(start = iconPadding)) {
                                Icon(Icons.Default.Search, null)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(collapse, Modifier.width(40.dp).alpha(closeAlpha)) {
                                Icon(Icons.Filled.Close, "Close", Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}