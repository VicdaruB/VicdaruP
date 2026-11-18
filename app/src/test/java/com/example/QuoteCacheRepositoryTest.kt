package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.QuoteCacheRepository
import com.example.data.local.VicdaruLocalDatabase
import com.example.model.AnimeSeries
import com.example.model.QuoteHistoryEntry
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuoteCacheRepositoryTest {

    private lateinit var db: VicdaruLocalDatabase
    private lateinit var cacheRepo: QuoteCacheRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = VicdaruLocalDatabase.createInMemory(context)
        cacheRepo = QuoteCacheRepository(db.cachedQuoteDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `syncFromRemote caches history and favorites per user ordered newest first`() = runBlocking {
        val older = QuoteHistoryEntry(
            id = "q_1",
            userId = "user_a",
            quoteText = "Todo estará bien",
            animeKey = AnimeSeries.SAKURA.key,
            characterName = "Sakura Kinomoto",
            moodTag = "Esperanza",
            favorite = false,
            createdAt = Timestamp(Date(10_000L))
        )
        val newerFav = QuoteHistoryEntry(
            id = "q_2",
            userId = "user_a",
            quoteText = "Los recuerdos brillan",
            animeKey = AnimeSeries.FRIEREN.key,
            characterName = "Frieren",
            moodTag = "Calma",
            favorite = true,
            createdAt = Timestamp(Date(20_000L))
        )
        val otherUserQuote = QuoteHistoryEntry(
            id = "q_other",
            userId = "user_b",
            quoteText = "Otro usuario",
            animeKey = AnimeSeries.RANMA.key,
            characterName = "Ranma",
            favorite = true,
            createdAt = Timestamp(Date(30_000L))
        )

        cacheRepo.syncFromRemote("user_a", listOf(older, newerFav))
        cacheRepo.syncFromRemote("user_b", listOf(otherUserQuote))

        val userAQuotes = cacheRepo.observeQuotes("user_a").first()
        val userAFavs = cacheRepo.observeFavoriteQuotes("user_a").first()

        assertEquals(listOf("q_2", "q_1"), userAQuotes.map { it.id })
        assertEquals(listOf("q_2"), userAFavs.map { it.id })
        assertEquals(1, cacheRepo.getCachedQuotes("user_b").size)
    }

    @Test
    fun `offline favorite toggle queues pending sync and survives stale remote overwrite`() = runBlocking {
        val remoteQuote = QuoteHistoryEntry(
            id = "q_10",
            userId = "user_a",
            quoteText = "Sigue adelante",
            animeKey = AnimeSeries.LITTLE_WITCH.key,
            characterName = "Akko",
            favorite = false,
            createdAt = Timestamp(Date(50_000L))
        )
        cacheRepo.syncFromRemote("user_a", listOf(remoteQuote))

        // Toggle favorite while offline
        cacheRepo.updateFavoriteLocal(
            userId = "user_a",
            quoteId = "q_10",
            favorite = true,
            pendingSync = true
        )

        assertEquals(1, cacheRepo.observePendingSyncCount("user_a").first())
        assertTrue(cacheRepo.observeFavoriteQuotes("user_a").first().any { it.id == "q_10" })

        // Even if a stale remote snapshot arrives before sync completes, local favorite choice persists
        cacheRepo.syncFromRemote("user_a", listOf(remoteQuote.copy(favorite = false)))
        val merged = cacheRepo.getCachedQuotes("user_a").first { it.id == "q_10" }
        assertTrue(merged.favorite)
        assertEquals(1, cacheRepo.getPendingSyncQuotes("user_a").size)

        // Mark synced after cloud update succeeds
        cacheRepo.markFavoriteSynced("user_a", "q_10")
        assertEquals(0, cacheRepo.observePendingSyncCount("user_a").first())
    }

    @Test
    fun `offline generated quote persists with pendingUpload and reconciles cleanly`() = runBlocking {
        val offlineQuote = QuoteHistoryEntry(
            id = "local_12345",
            userId = "user_a",
            quoteText = "Observa con calma",
            animeKey = AnimeSeries.APOTHECARY.key,
            characterName = "Maomao",
            favorite = true,
            sourceType = "MANUAL",
            createdAt = Timestamp(Date(90_000L))
        )

        cacheRepo.cacheQuote("user_a", offlineQuote, pendingUpload = true)
        assertEquals(1, cacheRepo.getPendingSyncQuotes("user_a").size)

        // Syncing empty or existing remote quotes must keep pendingUpload item intact
        cacheRepo.syncFromRemote("user_a", emptyList())
        assertEquals(listOf("local_12345"), cacheRepo.getCachedQuotes("user_a").map { it.id })

        // Replace temporary offline item with uploaded cloud entry while preserving offline favorite
        val cloudEntry = offlineQuote.copy(id = "q_cloud_99", favorite = false)
        cacheRepo.replacePendingUploadWithSynced(
            userId = "user_a",
            localTempId = "local_12345",
            syncedEntry = cloudEntry,
            preserveFavorite = true
        )

        val finalQuotes = cacheRepo.getCachedQuotes("user_a")
        assertEquals(listOf("q_cloud_99"), finalQuotes.map { it.id })
        assertTrue(finalQuotes.first().favorite)
        assertFalse(cacheRepo.getPendingSyncQuotes("user_a").any { it.pendingUpload })
    }
}
