package com.metrolist.desktop.personalization

enum class HomeSeedReason {
    SEARCH,
    ARTIST,
}

data class HomeSeed(
    val query: String,
    val title: String,
    val reason: HomeSeedReason,
)

data class PersonalizationSignals(
    val recentSearches: List<String> = emptyList(),
    val likedArtists: List<String> = emptyList(),
    val recentlyPlayedArtists: List<String> = emptyList(),
    val libraryArtists: List<String> = emptyList(),
)

object HomePersonalization {
    fun seeds(signals: PersonalizationSignals): List<HomeSeed> {
        val searches = signals.recentSearches
            .map(String::trim)
            .filter { it.length >= 2 }
            .distinctBy(String::lowercase)
            .take(MAX_SEARCH_SEEDS)
            .map { query ->
                HomeSeed(
                    query = query,
                    title = "Because you searched for “$query”",
                    reason = HomeSeedReason.SEARCH,
                )
            }

        val searchKeys = searches.mapTo(mutableSetOf()) { it.query.lowercase() }
        val scores = linkedMapOf<String, ArtistScore>()
        var sequence = 0

        fun addArtists(artists: List<String>, weight: Int) {
            artists.asSequence()
                .map(String::trim)
                .filter { it.isNotBlank() && !it.equals("Unknown artist", ignoreCase = true) }
                .forEach { artist ->
                    val key = artist.lowercase()
                    val existing = scores[key]
                    scores[key] = if (existing == null) {
                        ArtistScore(name = artist, score = weight, firstSeen = sequence++)
                    } else {
                        existing.copy(score = existing.score + weight)
                    }
                }
        }

        addArtists(signals.likedArtists, LIKED_WEIGHT)
        addArtists(signals.recentlyPlayedArtists, RECENT_PLAY_WEIGHT)
        addArtists(signals.libraryArtists, LIBRARY_WEIGHT)

        val artists = scores.values
            .asSequence()
            .filter { it.name.lowercase() !in searchKeys }
            .sortedWith(compareByDescending<ArtistScore> { it.score }.thenBy { it.firstSeen })
            .take((MAX_SEEDS - searches.size).coerceAtLeast(0))
            .map { artist ->
                HomeSeed(
                    query = artist.name,
                    title = "More from ${artist.name}",
                    reason = HomeSeedReason.ARTIST,
                )
            }
            .toList()

        return searches + artists
    }

    fun <T> blend(personal: List<T>, discovery: List<T>): List<T> {
        val selectedPersonal = personal.take(PERSONAL_SECTION_TARGET)
        val remaining = (MAX_HOME_SECTIONS - selectedPersonal.size).coerceAtLeast(0)
        return selectedPersonal + discovery.take(remaining)
    }

    private data class ArtistScore(
        val name: String,
        val score: Int,
        val firstSeen: Int,
    )

    private const val MAX_SEARCH_SEEDS = 2
    private const val MAX_SEEDS = 4
    private const val PERSONAL_SECTION_TARGET = 4
    private const val MAX_HOME_SECTIONS = 6
    private const val LIKED_WEIGHT = 6
    private const val RECENT_PLAY_WEIGHT = 4
    private const val LIBRARY_WEIGHT = 2
}
