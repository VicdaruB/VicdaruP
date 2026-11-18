package com.example.data.local

import com.example.model.QuoteHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class QuoteCacheRepository(private val dao: CachedQuoteDao) {

    fun observeQuotes(userId: String): Flow<List<QuoteHistoryEntry>> =
        dao.observeQuotesForUser(userId).map { list -> list.map { it.toQuoteHistoryEntry() } }

    fun observeFavoriteQuotes(userId: String): Flow<List<QuoteHistoryEntry>> =
        dao.observeFavoriteQuotesForUser(userId).map { list -> list.map { it.toQuoteHistoryEntry() } }

    fun observePendingSyncCount(userId: String): Flow<Int> =
        dao.observePendingSyncCount(userId)

    suspend fun getCachedQuotes(userId: String): List<QuoteHistoryEntry> = withContext(Dispatchers.IO) {
        dao.getQuotesForUser(userId).map { it.toQuoteHistoryEntry() }
    }

    suspend fun getPendingSyncQuotes(userId: String): List<CachedQuoteEntity> = withContext(Dispatchers.IO) {
        dao.getPendingSyncQuotes(userId)
    }

    suspend fun cacheQuote(
        userId: String,
        entry: QuoteHistoryEntry,
        pendingUpload: Boolean = false,
        pendingFavoriteSync: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        if (entry.id.isBlank()) return@withContext
        dao.upsert(
            entry.toCachedEntity(
                fallbackUserId = userId,
                pendingFavoriteSync = pendingFavoriteSync,
                pendingUpload = pendingUpload
            )
        )
    }

    suspend fun syncFromRemote(userId: String, remoteQuotes: List<QuoteHistoryEntry>) = withContext(Dispatchers.IO) {
        val entities = remoteQuotes
            .filter { it.id.isNotBlank() }
            .map { it.toCachedEntity(fallbackUserId = userId) }
        dao.replaceSyncedQuotes(userId, entities)
    }

    suspend fun updateFavoriteLocal(
        userId: String,
        quoteId: String,
        favorite: Boolean,
        pendingSync: Boolean,
    ) = withContext(Dispatchers.IO) {
        dao.updateFavoriteLocal(
            userId = userId,
            quoteId = quoteId,
            favorite = favorite,
            pendingSync = pendingSync,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun markFavoriteSynced(userId: String, quoteId: String) = withContext(Dispatchers.IO) {
        dao.markFavoriteSynced(userId, quoteId)
    }

    suspend fun replacePendingUploadWithSynced(
        userId: String,
        localTempId: String,
        syncedEntry: QuoteHistoryEntry,
        preserveFavorite: Boolean,
    ) = withContext(Dispatchers.IO) {
        dao.deleteById(userId, localTempId)
        dao.upsert(
            syncedEntry.toCachedEntity(
                fallbackUserId = userId,
                pendingFavoriteSync = preserveFavorite,
                pendingUpload = false
            ).copy(favorite = preserveFavorite)
        )
    }
}
