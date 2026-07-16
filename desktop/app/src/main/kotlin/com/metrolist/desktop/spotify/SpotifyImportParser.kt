package com.metrolist.desktop.spotify

data class SpotifyTrackReference(val id: String) {
    val publicUrl: String = "https://open.spotify.com/track/$id"
}

data class SpotifyImportParseResult(
    val tracks: List<SpotifyTrackReference>,
    val ignoredEntries: Int,
    val duplicateEntries: Int,
)

object SpotifyImportParser {
    private val trackPattern = Regex(
        "(?:https?://open\\.spotify\\.com/(?:intl-[a-z-]+/)?track/|spotify:track:)([A-Za-z0-9]{22})(?:\\?[^\\s]*)?",
        RegexOption.IGNORE_CASE,
    )

    fun parse(input: String): SpotifyImportParseResult {
        val matches = trackPattern.findAll(input).map { it.groupValues[1] }.toList()
        val uniqueIds = LinkedHashSet(matches)
        val ignoredEntries = input.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .count { line -> !trackPattern.containsMatchIn(line) }
        return SpotifyImportParseResult(
            tracks = uniqueIds.map(::SpotifyTrackReference),
            ignoredEntries = ignoredEntries,
            duplicateEntries = matches.size - uniqueIds.size,
        )
    }
}

data class SpotifyTrackMetadata(
    val reference: SpotifyTrackReference,
    val title: String,
    val artist: String,
    val album: String?,
    val year: Int?,
    val coverUrl: String?,
)

class SpotifyMetadataException(message: String) : IllegalArgumentException(message)

object SpotifyMetadataParser {
    private val metaTagPattern = Regex("<meta\\s+[^>]*>", RegexOption.IGNORE_CASE)
    private val attributePattern = Regex("([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*([\"'])(.*?)\\2")

    fun parse(reference: SpotifyTrackReference, html: String): SpotifyTrackMetadata {
        val properties = metaTagPattern.findAll(html).mapNotNull { match ->
            val attributes = attributePattern.findAll(match.value).associate { attribute ->
                attribute.groupValues[1].lowercase() to decodeHtml(attribute.groupValues[3])
            }
            val key = attributes["property"] ?: attributes["name"] ?: return@mapNotNull null
            val content = attributes["content"] ?: return@mapNotNull null
            key.lowercase() to content
        }.toMap()

        val title = properties["og:title"]?.trim().orEmpty()
        val descriptionParts = properties["og:description"]
            ?.split('·')
            ?.map(String::trim)
            .orEmpty()
        val artist = descriptionParts.firstOrNull().orEmpty()
        if (title.isBlank() || artist.isBlank()) {
            throw SpotifyMetadataException("Spotify did not return usable metadata for ${reference.id}")
        }
        return SpotifyTrackMetadata(
            reference = reference,
            title = title,
            artist = artist,
            album = descriptionParts.getOrNull(1)?.takeIf(String::isNotBlank),
            year = descriptionParts.lastOrNull()?.toIntOrNull(),
            coverUrl = properties["og:image"]?.takeIf(String::isNotBlank),
        )
    }

    private fun decodeHtml(value: String): String {
        var decoded = value
        repeat(2) {
            decoded = decoded
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#x27;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
        }
        return decoded
    }
}
