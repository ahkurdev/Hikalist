package com.metrolist.desktop.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CarouselNavigationTest {
    @Test
    fun `next advances one page`() {
        assertEquals(4, CarouselNavigation.nextIndex(firstVisibleIndex = 0, itemCount = 12))
    }

    @Test
    fun `next stops at the last item`() {
        assertEquals(11, CarouselNavigation.nextIndex(firstVisibleIndex = 9, itemCount = 12))
    }

    @Test
    fun `previous moves one page and stops at zero`() {
        assertEquals(3, CarouselNavigation.previousIndex(firstVisibleIndex = 7))
        assertEquals(0, CarouselNavigation.previousIndex(firstVisibleIndex = 2))
    }

    @Test
    fun `empty carousel stays at zero`() {
        assertEquals(0, CarouselNavigation.nextIndex(firstVisibleIndex = 0, itemCount = 0))
        assertEquals(0, CarouselNavigation.previousIndex(firstVisibleIndex = 0))
    }
}
