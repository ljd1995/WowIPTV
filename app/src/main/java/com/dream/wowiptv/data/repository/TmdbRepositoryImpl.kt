package com.dream.wowiptv.data.repository

import com.dream.wowiptv.data.remote.tmdb.TmdbApi
import com.dream.wowiptv.domain.repository.TmdbRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepositoryImpl @Inject constructor(
    private val tmdbApi: TmdbApi
) : TmdbRepository {

    private val cache = mutableMapOf<String, Map<String, String?>>()

    override suspend fun fetchPeopleImages(
        title: String?,
        releaseDate: String?,
        names: List<String>,
        apiKey: String
    ): Map<String, String?> {
        val cleaned = names.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isEmpty() || apiKey.isBlank()) return emptyMap()

        val cacheKey = "${title ?: ""}|${releaseDate ?: ""}"
        synchronized(cache) {
            cache[cacheKey]?.let { return it }
        }

        val year = releaseDate?.take(4)?.toIntOrNull()
        val creditsByName = if (!title.isNullOrBlank()) {
            try {
                val search = tmdbApi.searchMovie(apiKey, title)
                val movie = search.results.firstOrNull { it.release_date?.take(4)?.toIntOrNull() == year }
                    ?: search.results.firstOrNull()
                if (movie != null) {
                    tmdbApi.getMovieCredits(movie.id, apiKey)
                        .cast
                        .filter { !it.name.isNullOrBlank() }
                        .associate { it.name!!.trim() to profileUrl(it.profile_path) }
                } else {
                    emptyMap()
                }
            } catch (_: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }

        val result = linkedMapOf<String, String?>()
        for (name in cleaned) {
            val hit = creditsByName[name]
            if (hit != null) {
                result[name] = hit
            } else {
                result[name] = searchPersonPath(apiKey, name)
            }
        }

        synchronized(cache) {
            cache[cacheKey] = result
        }
        return result
    }

    private suspend fun searchPersonPath(apiKey: String, name: String): String? {
        return try {
            val response = tmdbApi.searchPerson(apiKey, name)
            response.results.firstOrNull()?.profile_path?.let { profileUrl(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun profileUrl(path: String?): String? =
        path?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w185$it" }
}
