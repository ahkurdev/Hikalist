package com.metrolist.desktop.window

import java.awt.Rectangle

enum class ResizeEdge {
    NONE,
    NORTH,
    NORTH_EAST,
    EAST,
    SOUTH_EAST,
    SOUTH,
    SOUTH_WEST,
    WEST,
    NORTH_WEST,
}

fun resizeEdgeAt(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    border: Int,
): ResizeEdge {
    if (width <= 0 || height <= 0 || border <= 0) return ResizeEdge.NONE
    val west = x in 0 until border
    val east = x >= width - border && x < width
    val north = y in 0 until border
    val south = y >= height - border && y < height
    return when {
        north && west -> ResizeEdge.NORTH_WEST
        north && east -> ResizeEdge.NORTH_EAST
        south && west -> ResizeEdge.SOUTH_WEST
        south && east -> ResizeEdge.SOUTH_EAST
        north -> ResizeEdge.NORTH
        east -> ResizeEdge.EAST
        south -> ResizeEdge.SOUTH
        west -> ResizeEdge.WEST
        else -> ResizeEdge.NONE
    }
}

fun resizedBounds(
    start: Rectangle,
    edge: ResizeEdge,
    deltaX: Int,
    deltaY: Int,
    minimumWidth: Int,
    minimumHeight: Int,
): Rectangle {
    val resizeWest = edge in setOf(ResizeEdge.WEST, ResizeEdge.NORTH_WEST, ResizeEdge.SOUTH_WEST)
    val resizeEast = edge in setOf(ResizeEdge.EAST, ResizeEdge.NORTH_EAST, ResizeEdge.SOUTH_EAST)
    val resizeNorth = edge in setOf(ResizeEdge.NORTH, ResizeEdge.NORTH_WEST, ResizeEdge.NORTH_EAST)
    val resizeSouth = edge in setOf(ResizeEdge.SOUTH, ResizeEdge.SOUTH_WEST, ResizeEdge.SOUTH_EAST)

    var x = start.x
    var y = start.y
    var width = start.width
    var height = start.height

    if (resizeWest) {
        val effectiveDelta = deltaX.coerceAtMost(start.width - minimumWidth)
        x = start.x + effectiveDelta
        width = start.width - effectiveDelta
    } else if (resizeEast) {
        width = (start.width + deltaX).coerceAtLeast(minimumWidth)
    }

    if (resizeNorth) {
        val effectiveDelta = deltaY.coerceAtMost(start.height - minimumHeight)
        y = start.y + effectiveDelta
        height = start.height - effectiveDelta
    } else if (resizeSouth) {
        height = (start.height + deltaY).coerceAtLeast(minimumHeight)
    }

    return Rectangle(x, y, width, height)
}
