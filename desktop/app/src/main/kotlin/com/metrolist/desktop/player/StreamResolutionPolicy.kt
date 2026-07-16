package com.metrolist.desktop.player

internal object StreamResolutionPolicy {
    data class Selection<C, V>(
        val candidate: C,
        val value: V,
    )

    suspend fun <C, V> select(
        candidates: List<C>,
        resolve: suspend (C) -> V?,
        validate: suspend (C, V) -> Boolean,
    ): Selection<C, V>? {
        for (candidate in candidates) {
            val value = resolve(candidate) ?: continue
            if (validate(candidate, value)) {
                return Selection(candidate, value)
            }
        }
        return null
    }
}
