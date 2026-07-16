package com.metrolist.desktop.ui

object PlayerUiGeometry {
    const val sliderThumbCorrectionDp = 3f

    fun showBottomPlayerBar(fullPlayerVisible: Boolean): Boolean = !fullPlayerVisible

    fun carouselArrowTopOffsetDp(
        artworkSizeDp: Float = HOME_ARTWORK_SIZE_DP,
        arrowSizeDp: Float = CAROUSEL_ARROW_SIZE_DP,
    ): Float = ((artworkSizeDp - arrowSizeDp) / 2f).coerceAtLeast(0f)

    private const val HOME_ARTWORK_SIZE_DP = 176f
    private const val CAROUSEL_ARROW_SIZE_DP = 46f
}
