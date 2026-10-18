package com.example.tracker.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Drag-and-drop reordering for some items of a `LazyColumn`, identified by their keys.
 *
 * While an item is dragged it follows the finger, and each time its middle crosses another
 * movable item [onMove] swaps them, so the caller's list (usually local UI state) shows the
 * new order straight away. [onDrop] is called once when the finger lifts, which is when the
 * caller should save the order.
 */
@Stable
class ReorderableListState internal constructor(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val canMove: (key: Any) -> Boolean,
    private val onMove: (fromKey: Any, toKey: Any) -> Unit,
    private val onDrop: (key: Any) -> Unit,
) {
    /** The key of the item being dragged, or null. */
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var draggedDistance by mutableFloatStateOf(0f)
    private var initialOffset by mutableIntStateOf(0)

    /**
     * The dragged item's index when it was last swapped. Touch events can arrive faster than
     * the list is laid out again; until the dragged item shows up at its new index, the layout
     * still describes the old order, and swapping again would undo the last swap.
     */
    private var swappedFromIndex: Int? = null

    /** How far the dragged item is drawn from where the list lays it out. */
    val draggingOffset: Float
        get() = draggingItem()?.let { initialOffset + draggedDistance - it.offset } ?: 0f

    internal fun onDragStart(key: Any): Boolean {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return false
        draggingKey = key
        initialOffset = item.offset
        draggedDistance = 0f
        swappedFromIndex = null
        return true
    }

    internal fun onDrag(deltaY: Float) {
        draggedDistance += deltaY
        val dragged = draggingItem() ?: return
        val top = dragged.offset + draggingOffset
        scrollIfAtEdge(top, top + dragged.size)
        if (dragged.index == swappedFromIndex) return
        swappedFromIndex = null

        val middle = (top + dragged.size / 2f).toInt()
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.key != dragged.key && canMove(item.key) && middle in item.offset..(item.offset + item.size)
        }
        if (target != null) {
            // The list keeps its first visible item in place; when that item takes part in the
            // swap, pin the scroll position so the list doesn't jump to follow it.
            if (dragged.index == listState.firstVisibleItemIndex || target.index == listState.firstVisibleItemIndex) {
                listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
            swappedFromIndex = dragged.index
            onMove(dragged.key, target.key)
        }
    }

    internal fun onDragStop() {
        val key = draggingKey ?: return
        draggingKey = null
        draggedDistance = 0f
        onDrop(key)
    }

    /** Scrolls the list while the dragged item is pushed past its top or bottom edge. */
    private fun scrollIfAtEdge(top: Float, bottom: Float) {
        val layout = listState.layoutInfo
        val overscroll = when {
            top < layout.viewportStartOffset -> top - layout.viewportStartOffset
            bottom > layout.viewportEndOffset -> bottom - layout.viewportEndOffset
            else -> 0f
        }
        if (overscroll != 0f) scope.launch { listState.scrollBy(overscroll) }
    }

    private fun draggingItem(): LazyListItemInfo? =
        draggingKey?.let { key -> listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } }
}

@Composable
fun rememberReorderableListState(
    listState: LazyListState,
    canMove: (key: Any) -> Boolean,
    onMove: (fromKey: Any, toKey: Any) -> Unit,
    onDrop: (key: Any) -> Unit,
): ReorderableListState {
    val scope = rememberCoroutineScope()
    // The callbacks close over the caller's latest list, so always call the newest ones.
    val currentCanMove by rememberUpdatedState(canMove)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDrop by rememberUpdatedState(onDrop)
    return remember(listState, scope) {
        ReorderableListState(
            listState = listState,
            scope = scope,
            canMove = { currentCanMove(it) },
            onMove = { from, to -> currentOnMove(from, to) },
            onDrop = { currentOnDrop(it) },
        )
    }
}

/**
 * An item that can be dragged: lifted above the others and following the finger while
 * dragged, sliding into place otherwise. [content] gets whether it is being dragged.
 */
@Composable
fun LazyItemScope.ReorderableItem(
    state: ReorderableListState,
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit,
) {
    val isDragging = state.draggingKey == key
    val placement = if (isDragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = state.draggingOffset }
    } else {
        Modifier.animateItem()
    }
    Box(modifier.then(placement)) { content(isDragging) }
}

/** Starts dragging [key] as soon as this element (a drag handle) is dragged. */
@Composable
fun Modifier.dragHandle(state: ReorderableListState, key: Any): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(state, key) {
        detectDragGestures(
            onDragStart = { if (state.onDragStart(key)) haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
            onDragEnd = state::onDragStop,
            onDragCancel = state::onDragStop,
            onDrag = { change, amount ->
                change.consume()
                state.onDrag(amount.y)
            },
        )
    }
}

/** Starts dragging [key] after a long press anywhere on this element. */
@Composable
fun Modifier.longPressDraggable(state: ReorderableListState, key: Any): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(state, key) {
        detectDragGesturesAfterLongPress(
            onDragStart = { if (state.onDragStart(key)) haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
            onDragEnd = state::onDragStop,
            onDragCancel = state::onDragStop,
            onDrag = { change, amount ->
                change.consume()
                state.onDrag(amount.y)
            },
        )
    }
}

/** [this] with the item keyed [fromKey] moved to where [toKey] is. */
fun <T> List<T>.moveByKey(fromKey: Any, toKey: Any, keyOf: (T) -> Any): List<T> {
    val from = indexOfFirst { keyOf(it) == fromKey }
    val to = indexOfFirst { keyOf(it) == toKey }
    if (from < 0 || to < 0 || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
