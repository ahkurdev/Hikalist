package com.metrolist.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.desktop.playlist.PlaylistCover
import com.metrolist.desktop.playlist.PlaylistCoverPolicy
import java.io.File

@Composable
fun PlaylistArtwork(
    customCover: String?,
    songThumbnails: List<String?>,
    size: Dp,
    cornerRadius: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    val cover = PlaylistCoverPolicy.resolve(customCover, songThumbnails)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        when (cover) {
            is PlaylistCover.Custom -> {
                val localFile = File(cover.url)
                ArtworkImage(if (localFile.isFile) localFile else cover.url)
            }
            is PlaylistCover.Mosaic -> {
                if (cover.urls.size == 1) ArtworkImage(cover.urls.first())
                else MosaicArtwork(cover.urls)
            }
            PlaylistCover.Fallback -> FlowerImage()
        }
    }
}

@Composable
fun HikalistLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource("hikalist-flower.png"),
        contentDescription = "Hikalist",
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.28f)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun FlowerImage() {
    Image(
        painter = painterResource("hikalist-flower.png"),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun MosaicArtwork(urls: List<String>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cell = maxWidth / 2
        Column(Modifier.fillMaxSize()) {
            repeat(2) { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Start) {
                    repeat(2) { column ->
                        val url = urls.getOrNull(row * 2 + column) ?: urls.getOrNull((row * 2 + column) % urls.size)
                        Box(Modifier.size(cell).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            if (url != null) ArtworkImage(url)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkImage(model: Any?) {
    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
    )
}
