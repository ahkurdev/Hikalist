package com.metrolist.desktop.window

import java.awt.Rectangle
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowResizeGeometryTest {
    @Test
    fun `hit testing identifies edges corners and center`() {
        assertEquals(ResizeEdge.NORTH_WEST, resizeEdgeAt(2, 2, 1_000, 600, 8))
        assertEquals(ResizeEdge.NORTH_EAST, resizeEdgeAt(998, 2, 1_000, 600, 8))
        assertEquals(ResizeEdge.SOUTH_WEST, resizeEdgeAt(2, 598, 1_000, 600, 8))
        assertEquals(ResizeEdge.SOUTH_EAST, resizeEdgeAt(998, 598, 1_000, 600, 8))
        assertEquals(ResizeEdge.NORTH, resizeEdgeAt(500, 2, 1_000, 600, 8))
        assertEquals(ResizeEdge.WEST, resizeEdgeAt(2, 300, 1_000, 600, 8))
        assertEquals(ResizeEdge.NONE, resizeEdgeAt(500, 300, 1_000, 600, 8))
    }

    @Test
    fun `south east resize grows from the anchored top left`() {
        val resized = resizedBounds(
            start = Rectangle(100, 80, 1_000, 600),
            edge = ResizeEdge.SOUTH_EAST,
            deltaX = 120,
            deltaY = 75,
            minimumWidth = 900,
            minimumHeight = 560,
        )

        assertEquals(Rectangle(100, 80, 1_120, 675), resized)
    }

    @Test
    fun `north west resize clamps size and moves only by effective delta`() {
        val resized = resizedBounds(
            start = Rectangle(100, 80, 1_000, 600),
            edge = ResizeEdge.NORTH_WEST,
            deltaX = 400,
            deltaY = 300,
            minimumWidth = 900,
            minimumHeight = 560,
        )

        assertEquals(Rectangle(200, 120, 900, 560), resized)
    }

    @Test
    fun `west resize expands left while keeping right edge anchored`() {
        val resized = resizedBounds(
            start = Rectangle(100, 80, 1_000, 600),
            edge = ResizeEdge.WEST,
            deltaX = -75,
            deltaY = 0,
            minimumWidth = 900,
            minimumHeight = 560,
        )

        assertEquals(Rectangle(25, 80, 1_075, 600), resized)
    }
}
