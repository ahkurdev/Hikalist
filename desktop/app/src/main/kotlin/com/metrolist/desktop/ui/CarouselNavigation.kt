package com.metrolist.desktop.ui

object CarouselNavigation {
    fun nextIndex(
        firstVisibleIndex: Int,
        itemCount: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Int {
        if (itemCount <= 0) return 0
        return (firstVisibleIndex + pageSize.coerceAtLeast(1)).coerceAtMost(itemCount - 1)
    }

    fun previousIndex(
        firstVisibleIndex: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): Int = (firstVisibleIndex - pageSize.coerceAtLeast(1)).coerceAtLeast(0)

    private const val DEFAULT_PAGE_SIZE = 4
}
