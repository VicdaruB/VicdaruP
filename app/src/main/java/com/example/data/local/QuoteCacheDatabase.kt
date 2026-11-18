package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.model.QuoteHistoryEntry
import com.google.firebase.Timestamp
import java.util.Date

@Entity(
    tableName = "cached_quotes",
    indices = [Index(value = ["userId", "createdAtMillis"])]
)
data class CachedQuoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val quoteText: String,
    val animeKey: String,
    val characterName: String,
    val moodTag: String,
    val imageUrl: String,
    val imageSource: String,
    val favorite: Boolean,
    val sourceType: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val pendingFavoriteSync: Boolean = false,
    val pendingUpload: Boolean = false,
) {
    fun toQuoteHistoryEntry(): QuoteHistoryEntry = QuoteHistoryEntry(
        id = id,
        userId = userId,
        quoteText = quoteText,
        animeKey = animeKey,
        characterName = characterName,
        moodTag = moodTag,
        imageUrl = imageUrl,
        imageSource = imageSource,
        favorite = favorite,
        sourceType = sourceType,
        createdAt = Timestamp(Date(createdAtMillis)),
        updatedAt = Timestamp(Date(updatedAtMillis))
    )
}

fun QuoteHistoryEntry.toCachedEntity(
    fallbackUserId: String,
    pendingFavoriteSync: Boolean = false,
    pendingUpload: Boolean = false,
): CachedQuoteEntity {
    val now = System.currentTimeMillis()
    val createdMs = createdAt?.toDate()?.time ?: now
    val updatedMs = updatedAt?.toDate()?.time ?: createdMs
    return CachedQuoteEntity(
        id = id,
        userId = userId.ifBlank { fallbackUserId },
        quoteText = quoteText,
        animeKey = animeKey,
        characterName = characterName,
        moodTag = moodTag,
        imageUrl = imageUrl,
        imageSource = imageSource,
        favorite = favorite,
        sourceType = sourceType,
        createdAtMillis = createdMs,
        updatedAtMillis = updatedMs,
        pendingFavoriteSync = pendingFavoriteSync,
        pendingUpload = pendingUpload
    )
}

@Dao
interface CachedQuoteDao {
    @Query("SELECT * FROM cached_quotes WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeQuotesForUser(userId: String): kotlinx.coroutines.flow.Flow<List<CachedQuoteEntity>>

    @Query("SELECT * FROM cached_quotes WHERE userId = :userId AND favorite = 1 ORDER BY createdAtMillis DESC")
    fun observeFavoriteQuotesForUser(userId: String): kotlinx.coroutines.flow.Flow<List<CachedQuoteEntity>>

    @Query("SELECT COUNT(*) FROM cached_quotes WHERE userId = :userId AND (pendingFavoriteSync = 1 OR pendingUpload = 1)")
    fun observePendingSyncCount(userId: String): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT * FROM cached_quotes WHERE userId = :userId ORDER BY createdAtMillis DESC")
    suspend fun getQuotesForUser(userId: String): List<CachedQuoteEntity>

    @Query("SELECT * FROM cached_quotes WHERE userId = :userId AND (pendingFavoriteSync = 1 OR pendingUpload = 1) ORDER BY createdAtMillis ASC")
    suspend fun getPendingSyncQuotes(userId: String): List<CachedQuoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quote: CachedQuoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(quotes: List<CachedQuoteEntity>)

    @Query(
        "UPDATE cached_quotes SET favorite = :favorite, pendingFavoriteSync = :pendingSync, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :quoteId AND userId = :userId"
    )
    suspend fun updateFavoriteLocal(
        userId: String,
        quoteId: String,
        favorite: Boolean,
        pendingSync: Boolean,
        updatedAtMillis: Long,
    )

    @Query("UPDATE cached_quotes SET pendingFavoriteSync = 0 WHERE id = :quoteId AND userId = :userId")
    suspend fun markFavoriteSynced(userId: String, quoteId: String)

    @Query("DELETE FROM cached_quotes WHERE id = :quoteId AND userId = :userId")
    suspend fun deleteById(userId: String, quoteId: String)

    @Query("DELETE FROM cached_quotes WHERE userId = :userId AND pendingUpload = 0")
    suspend fun clearSyncedForUser(userId: String)

    @Query("DELETE FROM cached_quotes WHERE userId = :userId AND pendingUpload = 0 AND id NOT IN (:activeIds)")
    suspend fun deleteRemovedRemoteQuotes(userId: String, activeIds: List<String>)

    @Transaction
    suspend fun replaceSyncedQuotes(userId: String, remoteQuotes: List<CachedQuoteEntity>) {
        if (remoteQuotes.isEmpty()) {
            clearSyncedForUser(userId)
            return
        }
        val existingPendingFavs = getQuotesForUser(userId)
            .filter { it.pendingFavoriteSync }
            .associateBy { it.id }

        val mergedRemote = remoteQuotes.map { remote ->
            val pending = existingPendingFavs[remote.id]
            if (pending != null) {
                remote.copy(
                    favorite = pending.favorite,
                    pendingFavoriteSync = true,
                    updatedAtMillis = maxOf(remote.updatedAtMillis, pending.updatedAtMillis)
                )
            } else {
                remote
            }
        }
        deleteRemovedRemoteQuotes(userId, mergedRemote.map { it.id })
        upsertAll(mergedRemote)
    }
}

@Database(entities = [CachedQuoteEntity::class], version = 1, exportSchema = false)
abstract class VicdaruLocalDatabase : RoomDatabase() {
    abstract fun cachedQuoteDao(): CachedQuoteDao

    companion object {
        @Volatile
        private var INSTANCE: VicdaruLocalDatabase? = null

        fun getInstance(context: Context): VicdaruLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VicdaruLocalDatabase::class.java,
                    "vicdaru_local_cache.db"
                ).build().also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): VicdaruLocalDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                VicdaruLocalDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
