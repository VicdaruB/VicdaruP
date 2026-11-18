package com.example.network

import com.example.model.AnimeSeries
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@JsonClass(generateAdapter = true)
data class JikanPicturesResponse(
    val data: List<JikanPictureEntry> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class JikanPictureEntry(
    val jpg: JikanImageSet? = null,
    val webp: JikanImageSet? = null,
)

@JsonClass(generateAdapter = true)
data class JikanImageSet(
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "large_image_url") val largeImageUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class JikanCharactersResponse(
    val data: List<JikanCharacterWrapper> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class JikanCharacterWrapper(
    val character: JikanCharacterInfo? = null,
)

@JsonClass(generateAdapter = true)
data class JikanCharacterInfo(
    @Json(name = "mal_id") val malId: Int? = null,
    val name: String? = null,
    val images: JikanCharacterImages? = null,
)

@JsonClass(generateAdapter = true)
data class JikanCharacterImages(
    val jpg: JikanImageSet? = null,
    val webp: JikanImageSet? = null,
)

interface JikanApiService {
    @GET("anime/{id}/pictures")
    suspend fun getAnimePictures(@Path("id") malId: Int): JikanPicturesResponse

    @GET("anime/{id}/characters")
    suspend fun getAnimeCharacters(@Path("id") malId: Int): JikanCharactersResponse

    @GET("characters/{id}/pictures")
    suspend fun getCharacterPictures(@Path("id") characterId: Int): JikanPicturesResponse
}

data class FetchedAnimeImage(
    val imageUrl: String,
    val sourceLabel: String,
)

object AnimeImageFetcher {
    private val poolByAnime = ConcurrentHashMap<String, MutableList<FetchedAnimeImage>>()
    private val pageByAnime = ConcurrentHashMap<String, Int>()
    private val sessionUsedUrls = ConcurrentHashMap.newKeySet<String>()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val jikanApi: JikanApiService by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl("https://api.jikan.moe/v4/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JikanApiService::class.java)
    }

    private fun isRenderableImageUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (!url.startsWith("https://") || url.length > 600) return false
        val lower = url.lowercase()
        if (lower.contains("questionmark")) return false
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".zip") || lower.endsWith(".swf")) {
            return false
        }
        return true
    }

    private fun AnimeSeries.booruTags(): List<String> = when (this) {
        AnimeSeries.FRIEREN -> listOf("sousou_no_frieren", "frieren")
        AnimeSeries.APOTHECARY -> listOf("kusuriya_no_hitorigoto", "maomao_(kusuriya_no_hitorigoto)")
        AnimeSeries.DANDADAN -> listOf("dandadan", "ayase_momo")
        AnimeSeries.LITTLE_WITCH -> listOf("little_witch_academia", "kagari_atsuko")
        AnimeSeries.SAKURA -> listOf("cardcaptor_sakura", "kinomoto_sakura")
        AnimeSeries.RANMA -> listOf("ranma_1/2", "saotome_ranma")
    }

    private fun AnimeSeries.searchQuery(): String = when (this) {
        AnimeSeries.FRIEREN -> "frieren"
        AnimeSeries.APOTHECARY -> "kusuriya no hitorigoto"
        AnimeSeries.DANDADAN -> "dandadan"
        AnimeSeries.LITTLE_WITCH -> "little witch academia"
        AnimeSeries.SAKURA -> "cardcaptor sakura"
        AnimeSeries.RANMA -> "ranma"
    }

    suspend fun fetchFreshImage(
        anime: AnimeSeries,
        excludeUrls: Set<String> = emptySet(),
        characterHint: String? = null,
    ): FetchedAnimeImage = withContext(Dispatchers.IO) {
        val allExcluded = (excludeUrls + sessionUsedUrls).toSet()
        val pool = poolByAnime.getOrPut(anime.key) { mutableListOf() }

        var available = synchronized(pool) {
            pool.filter { it.imageUrl !in allExcluded }
        }

        // If unused pool is empty or small, query next pages across all external sources
        if (available.size < 4) {
            val nextPage = (pageByAnime[anime.key] ?: 0) + 1
            pageByAnime[anime.key] = nextPage
            val freshBatch = fetchBatchFromAllSources(anime, nextPage, characterHint)
            synchronized(pool) {
                val existingUrls = pool.map { it.imageUrl }.toHashSet()
                freshBatch.forEach { candidate ->
                    if (existingUrls.add(candidate.imageUrl)) {
                        pool.add(candidate)
                    }
                }
                available = pool.filter { it.imageUrl !in allExcluded }
            }
        }

        // Second pass on random deeper page if everything on this page was already used
        if (available.isEmpty()) {
            val randomDeepPage = Random.nextInt(2, 9)
            val deeperBatch = fetchBatchFromAllSources(anime, randomDeepPage, characterHint)
            synchronized(pool) {
                val existingUrls = pool.map { it.imageUrl }.toHashSet()
                deeperBatch.forEach { candidate ->
                    if (existingUrls.add(candidate.imageUrl)) {
                        pool.add(candidate)
                    }
                }
                available = pool.filter { it.imageUrl !in allExcluded }
            }
        }

        val picked = if (available.isNotEmpty()) {
            available.random()
        } else {
            val anyPool = synchronized(pool) { pool.toList() }
            anyPool.firstOrNull { it.imageUrl !in excludeUrls }
                ?: anyPool.randomOrNull()
                ?: FetchedAnimeImage(anime.fallbackImageUrl, "Archivo Oficial (${anime.displayName})")
        }

        sessionUsedUrls.add(picked.imageUrl)
        picked
    }

    suspend fun fetchRandomImageUrl(anime: AnimeSeries, previousUrl: String? = null): String {
        val exclude = if (previousUrl.isNullOrBlank()) emptySet() else setOf(previousUrl)
        return fetchFreshImage(anime, exclude).imageUrl
    }

    private suspend fun fetchBatchFromAllSources(
        anime: AnimeSeries,
        page: Int,
        characterHint: String?,
    ): List<FetchedAnimeImage> = coroutineScope {
        val tag = anime.booruTags()[(page - 1) % anime.booruTags().size]
        val altTag = anime.booruTags().first()
        val query = if (!characterHint.isNullOrBlank() && page % 2 == 0) {
            "${anime.searchQuery()} ${characterHint.split(" ").firstOrNull().orEmpty()}".trim()
        } else {
            anime.searchQuery()
        }

        val tasks = listOf(
            async { fetchFromJikan(anime, page) },
            async { fetchFromAniList(anime, page) },
            async { fetchFromDanbooru(tag, page) },
            async { fetchFromSafebooru(altTag, page - 1) },
            async { fetchFromKonachan(altTag, page) },
            async { fetchFromYandere(altTag, page) },
            async { fetchFromWallhaven(query, page) },
        )

        tasks.awaitAll().flatten().distinctBy { it.imageUrl }.shuffled()
    }

    private suspend fun fetchFromJikan(anime: AnimeSeries, page: Int): List<FetchedAnimeImage> {
        val list = mutableListOf<FetchedAnimeImage>()
        try {
            val pics = jikanApi.getAnimePictures(anime.malId)
            pics.data.forEach { entry ->
                val url = entry.jpg?.largeImageUrl
                    ?: entry.jpg?.imageUrl
                    ?: entry.webp?.largeImageUrl
                    ?: entry.webp?.imageUrl
                if (isRenderableImageUrl(url)) {
                    list.add(FetchedAnimeImage(url!!, "Jikan • Arte Oficial"))
                }
            }
        } catch (_: Exception) {
        }

        try {
            val chars = jikanApi.getAnimeCharacters(anime.malId).data
            chars.take(40).forEach { wrapper ->
                val url = wrapper.character?.images?.jpg?.imageUrl
                    ?: wrapper.character?.images?.webp?.imageUrl
                if (isRenderableImageUrl(url)) {
                    val charName = wrapper.character?.name?.take(24) ?: "Personaje"
                    list.add(FetchedAnimeImage(url!!, "Jikan • Personaje ($charName)"))
                }
            }
            val startIdx = ((page - 1) * 2) % chars.size.coerceAtLeast(1)
            chars.drop(startIdx).take(2).forEach { wrapper ->
                val charId = wrapper.character?.malId ?: return@forEach
                try {
                    val charPics = jikanApi.getCharacterPictures(charId)
                    charPics.data.forEach { cp ->
                        val cUrl = cp.jpg?.largeImageUrl ?: cp.jpg?.imageUrl
                        if (isRenderableImageUrl(cUrl)) {
                            list.add(FetchedAnimeImage(cUrl!!, "Jikan • Galería Personaje"))
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun fetchFromAniList(anime: AnimeSeries, page: Int): List<FetchedAnimeImage> {
        val list = mutableListOf<FetchedAnimeImage>()
        try {
            val gql = """
                query (${'$'}malId: Int, ${'$'}page: Int) {
                  Media(idMal: ${'$'}malId, type: ANIME) {
                    bannerImage
                    coverImage { extraLarge large }
                    characters(page: ${'$'}page, perPage: 25) {
                      nodes {
                        name { full }
                        image { large }
                      }
                    }
                  }
                }
            """.trimIndent()
            val payload = JSONObject().apply {
                put("query", gql)
                put("variables", JSONObject().apply {
                    put("malId", anime.malId)
                    put("page", ((page - 1) % 3) + 1)
                })
            }
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return list
                val body = resp.body?.string() ?: return list
                val media = JSONObject(body).optJSONObject("data")?.optJSONObject("Media") ?: return list
                val banner = media.optString("bannerImage")
                if (isRenderableImageUrl(banner)) {
                    list.add(FetchedAnimeImage(banner, "AniList • Banner Oficial"))
                }
                val coverObj = media.optJSONObject("coverImage")
                listOfNotNull(coverObj?.optString("extraLarge"), coverObj?.optString("large")).forEach { u ->
                    if (isRenderableImageUrl(u)) {
                        list.add(FetchedAnimeImage(u, "AniList • Portada Oficial"))
                    }
                }
                val nodes = media.optJSONObject("characters")?.optJSONArray("nodes")
                if (nodes != null) {
                    for (i in 0 until nodes.length()) {
                        val node = nodes.optJSONObject(i) ?: continue
                        val img = node.optJSONObject("image")?.optString("large")
                        val cname = node.optJSONObject("name")?.optString("full")?.take(24) ?: "Personaje"
                        if (isRenderableImageUrl(img)) {
                            list.add(FetchedAnimeImage(img!!, "AniList • $cname"))
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun fetchFromDanbooru(tag: String, page: Int): List<FetchedAnimeImage> {
        val list = mutableListOf<FetchedAnimeImage>()
        try {
            val encoded = URLEncoder.encode(tag, "UTF-8")
            val url = "https://danbooru.donmai.us/posts.json?tags=$encoded&limit=40&page=$page"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "VicdaruProject/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return list
                val arr = JSONArray(resp.body?.string() ?: return list)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val imgUrl = obj.optString("large_file_url")
                        .ifBlank { obj.optString("file_url") }
                        .ifBlank { obj.optString("preview_file_url") }
                    if (isRenderableImageUrl(imgUrl)) {
                        val artist = obj.optString("tag_string_artist").split(" ").firstOrNull().orEmpty()
                        val label = if (artist.isNotBlank()) "Danbooru • Fanart ($artist)" else "Danbooru • Fanart / Arte"
                        list.add(FetchedAnimeImage(imgUrl, label.take(75)))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun fetchFromSafebooru(tag: String, pid: Int): List<FetchedAnimeImage> {
        val list = mutableListOf<FetchedAnimeImage>()
        try {
            val encoded = URLEncoder.encode(tag, "UTF-8")
            val url = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&tags=$encoded&limit=40&pid=$pid"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "VicdaruProject/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return list
                val raw = resp.body?.string()?.trim() ?: return list
                if (!raw.startsWith("[")) return list
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val fileUrl = obj.optString("file_url").ifBlank {
                        val dir = obj.optString("directory")
                        val image = obj.optString("image")
                        if (dir.isNotBlank() && image.isNotBlank()) "https://safebooru.org/images/$dir/$image" else ""
                    }
                    if (isRenderableImageUrl(fileUrl)) {
                        list.add(FetchedAnimeImage(fileUrl, "Safebooru • Fanart & Personajes"))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun fetchFromKonachan(tag: String, page: Int): List<FetchedAnimeImage> {
        val list = mutableListOf<FetchedAnimeImage>()
        try {
            val encoded = URLEncoder.encode(tag, "UTF-8")
            val url = "https://konachan.net/post.json?tags=$encoded&limit=40&page=$page"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "VicdaruProject/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return list
                val arr = JSONArray(resp.body?.string() ?: return list)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val imgUrl = obj.optString("sample_url")
                        .ifBlank { obj.optString("jpeg_url") }
                        .ifBlank { obj.optString("file_url") }
                    if (isRenderableImageUrl(imgUrl)) {
                        list.add(FetchedAnimeImage(imgUrl, "Konachan • Ilustración"))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun fetchFromYandere(tag: String, page: Int): List<FetchedAnimeImage> {
        val list = mutableListOf<FetchedAnimeImage>()
        try {
            val encoded = URLEncoder.encode(tag, "UTF-8")
            val url = "https://yande.re/post.json?tags=$encoded&limit=35&page=$page"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "VicdaruProject/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return list
                val arr = JSONArray(resp.body?.string() ?: return list)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val imgUrl = obj.optString("sample_url")
                        .ifBlank { obj.optString("jpeg_url") }
                        .ifBlank { obj.optString("file_url") }
                    if (isRenderableImageUrl(imgUrl)) {
                        list.add(FetchedAnimeImage(imgUrl, "Yande.re • Fanart & Arte"))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun fetchFromWallhaven(query: String, page: Int): List<FetchedAnimeImage> {
        val list = mutableListOf<FetchedAnimeImage>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://wallhaven.cc/api/v1/search?q=$encoded&categories=010&sorting=random&page=$page"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "VicdaruProject/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return list
                val data = JSONObject(resp.body?.string() ?: return list).optJSONArray("data") ?: return list
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    val path = obj.optString("path").ifBlank {
                        obj.optJSONObject("thumbs")?.optString("large").orEmpty()
                    }
                    if (isRenderableImageUrl(path)) {
                        val res = obj.optString("resolution")
                        val label = if (res.isNotBlank()) "Wallhaven • Arte ($res)" else "Wallhaven • Arte Anime"
                        list.add(FetchedAnimeImage(path, label.take(75)))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }
}
