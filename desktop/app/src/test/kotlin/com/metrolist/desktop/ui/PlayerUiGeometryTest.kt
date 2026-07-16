package com.metrolist.desktop.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerUiGeometryTest {
    @Test
    fun `carousel arrow is centered against artwork instead of variable card height`() {
        assertEquals(
            65f,
            PlayerUiGeometry.carouselArrowTopOffsetDp(artworkSizeDp = 176f, arrowSizeDp = 46f),
            0f,
        )
    }

    @Test
    fun `custom slider thumb is lowered to match the visual track center`() {
        assertEquals(3f, PlayerUiGeometry.sliderThumbCorrectionDp, 0f)
    }

    @Test
    fun `bottom player bar is hidden only while full player is open`() {
        assertEquals(true, PlayerUiGeometry.showBottomPlayerBar(fullPlayerVisible = false))
        assertEquals(false, PlayerUiGeometry.showBottomPlayerBar(fullPlayerVisible = true))
    }
}
