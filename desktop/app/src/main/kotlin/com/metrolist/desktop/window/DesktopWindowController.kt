package com.metrolist.desktop.window

import java.awt.AWTEvent
import java.awt.Cursor
import java.awt.Frame
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class DesktopWindowController(
    private val frame: Frame,
    private val windowState: WindowState,
    private val minimumWidth: Int = 900,
    private val minimumHeight: Int = 560,
    private val resizeBorder: Int = 8,
) : AutoCloseable {
    private var installed = false
    private var activeResizeEdge = ResizeEdge.NONE
    private var resizeStartPointer: Point? = null
    private var resizeStartBounds: Rectangle? = null
    private var moveStartPointer: Point? = null
    private var moveStartBounds: Rectangle? = null
    private var restoredBounds: Rectangle? = null
    private var dragRemainderX = 0f
    private var dragRemainderY = 0f
    private val _maximized = MutableStateFlow(false)
    val maximized: StateFlow<Boolean> = _maximized.asStateFlow()

    private val mouseListener = AWTEventListener { event ->
        if (event is MouseEvent && belongsToFrame(event)) handleMouseEvent(event)
    }

    fun install() {
        if (installed) return
        Toolkit.getDefaultToolkit().addAWTEventListener(
            mouseListener,
            AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK,
        )
        installed = true
    }

    fun minimize() {
        windowState.isMinimized = true
    }

    fun toggleMaximize() {
        if (_maximized.value) restore() else maximize()
    }

    fun restoreForDrag(anchorFraction: Float) {
        if (!_maximized.value) return
        val pointer = MouseInfo.getPointerInfo()?.location ?: return restore()
        val target = restoredBounds ?: Rectangle(
            frame.x,
            frame.y,
            minimumWidth.coerceAtLeast(frame.width / 2),
            minimumHeight.coerceAtLeast(frame.height / 2),
        )
        _maximized.value = false
        windowState.placement = WindowPlacement.Floating
        val fraction = anchorFraction.coerceIn(0.05f, 0.95f)
        target.x = pointer.x - (target.width * fraction).roundToInt()
        target.y = pointer.y - TITLE_BAR_DRAG_OFFSET
        applyBounds(target)
    }

    fun moveBy(deltaX: Float, deltaY: Float) {
        if (_maximized.value) return
        dragRemainderX += deltaX
        dragRemainderY += deltaY
        val moveX = dragRemainderX.roundToInt()
        val moveY = dragRemainderY.roundToInt()
        if (moveX == 0 && moveY == 0) return
        dragRemainderX -= moveX
        dragRemainderY -= moveY
        applyBounds(Rectangle(frame.x + moveX, frame.y + moveY, frame.width, frame.height))
    }

    private fun maximize() {
        restoredBounds = frame.bounds
        val graphicsConfiguration = frame.graphicsConfiguration
        val screen = graphicsConfiguration.bounds
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration)
        applyBounds(
            Rectangle(
                screen.x + insets.left,
                screen.y + insets.top,
                screen.width - insets.left - insets.right,
                screen.height - insets.top - insets.bottom,
            ),
        )
        _maximized.value = true
        frame.cursor = Cursor.getDefaultCursor()
    }

    private fun restore() {
        windowState.placement = WindowPlacement.Floating
        restoredBounds?.let { applyBounds(Rectangle(it)) }
        _maximized.value = false
    }

    private fun belongsToFrame(event: MouseEvent): Boolean {
        val component = event.component ?: return false
        return component === frame || SwingUtilities.getWindowAncestor(component) === frame
    }

    private fun handleMouseEvent(event: MouseEvent) {
        if (_maximized.value) {
            if (event.id == MouseEvent.MOUSE_MOVED) frame.cursor = Cursor.getDefaultCursor()
        }
        when (event.id) {
            MouseEvent.MOUSE_MOVED -> updateResizeCursor(event)
            MouseEvent.MOUSE_PRESSED -> beginPointerAction(event)
            MouseEvent.MOUSE_DRAGGED -> if (activeResizeEdge != ResizeEdge.NONE) resize(event) else move(event)
            MouseEvent.MOUSE_RELEASED -> finishPointerAction()
            MouseEvent.MOUSE_EXITED -> if (activeResizeEdge == ResizeEdge.NONE) {
                frame.cursor = Cursor.getDefaultCursor()
            }
        }
    }

    private fun updateResizeCursor(event: MouseEvent) {
        val edge = if (_maximized.value) ResizeEdge.NONE else edgeAt(event)
        frame.cursor = Cursor.getPredefinedCursor(edge.cursorType())
    }

    private fun beginPointerAction(event: MouseEvent) {
        if (event.button != MouseEvent.BUTTON1) return
        val edge = if (_maximized.value) ResizeEdge.NONE else edgeAt(event)
        if (edge != ResizeEdge.NONE) {
            activeResizeEdge = edge
            resizeStartPointer = event.locationOnScreen
            resizeStartBounds = Rectangle(frame.bounds)
            event.consume()
            return
        }

        val localX = event.xOnScreen - frame.x
        val localY = event.yOnScreen - frame.y
        val titleArea = localY in 0 until titleBarHeight && localX < frame.width - windowControlAreaWidth
        if (!titleArea) return
        if (event.clickCount >= 2) {
            toggleMaximize()
            event.consume()
            return
        }
        if (_maximized.value) restoreForDrag(localX.toFloat() / frame.width.coerceAtLeast(1))
        moveStartPointer = event.locationOnScreen
        moveStartBounds = Rectangle(frame.bounds)
        event.consume()
    }

    private fun resize(event: MouseEvent) {
        val pointer = resizeStartPointer ?: return
        val bounds = resizeStartBounds ?: return
        applyBounds(resizedBounds(
            start = bounds,
            edge = activeResizeEdge,
            deltaX = event.xOnScreen - pointer.x,
            deltaY = event.yOnScreen - pointer.y,
            minimumWidth = minimumWidth,
            minimumHeight = minimumHeight,
        ))
        event.consume()
    }

    private fun move(event: MouseEvent) {
        val pointer = moveStartPointer ?: return
        val bounds = moveStartBounds ?: return
        applyBounds(
            Rectangle(
                bounds.x + event.xOnScreen - pointer.x,
                bounds.y + event.yOnScreen - pointer.y,
                bounds.width,
                bounds.height,
            ),
        )
        event.consume()
    }

    private fun finishPointerAction() {
        activeResizeEdge = ResizeEdge.NONE
        resizeStartPointer = null
        resizeStartBounds = null
        moveStartPointer = null
        moveStartBounds = null
    }

    private fun edgeAt(event: MouseEvent): ResizeEdge = resizeEdgeAt(
        x = event.xOnScreen - frame.x,
        y = event.yOnScreen - frame.y,
        width = frame.width,
        height = frame.height,
        border = resizeBorder,
    )

    override fun close() {
        if (!installed) return
        Toolkit.getDefaultToolkit().removeAWTEventListener(mouseListener)
        installed = false
    }

    private fun applyBounds(bounds: Rectangle) {
        windowState.position = WindowPosition.Absolute(
            x = bounds.x.toFloat().dp,
            y = bounds.y.toFloat().dp,
        )
        windowState.size = DpSize(
            width = bounds.width.toFloat().dp,
            height = bounds.height.toFloat().dp,
        )
        frame.bounds = bounds
    }

    private fun ResizeEdge.cursorType(): Int = when (this) {
        ResizeEdge.NORTH -> Cursor.N_RESIZE_CURSOR
        ResizeEdge.NORTH_EAST -> Cursor.NE_RESIZE_CURSOR
        ResizeEdge.EAST -> Cursor.E_RESIZE_CURSOR
        ResizeEdge.SOUTH_EAST -> Cursor.SE_RESIZE_CURSOR
        ResizeEdge.SOUTH -> Cursor.S_RESIZE_CURSOR
        ResizeEdge.SOUTH_WEST -> Cursor.SW_RESIZE_CURSOR
        ResizeEdge.WEST -> Cursor.W_RESIZE_CURSOR
        ResizeEdge.NORTH_WEST -> Cursor.NW_RESIZE_CURSOR
        ResizeEdge.NONE -> Cursor.DEFAULT_CURSOR
    }

    private companion object {
        const val TITLE_BAR_DRAG_OFFSET = 18
        const val DEFAULT_TITLE_BAR_HEIGHT = 38
        const val DEFAULT_WINDOW_CONTROL_AREA_WIDTH = 3 * 46
    }

    private val titleBarHeight: Int
        get() = DEFAULT_TITLE_BAR_HEIGHT

    private val windowControlAreaWidth: Int
        get() = DEFAULT_WINDOW_CONTROL_AREA_WIDTH
}
