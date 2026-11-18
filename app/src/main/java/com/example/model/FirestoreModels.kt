package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val darkMode: Boolean = true,
    val morningQuoteEnabled: Boolean = true,
    val morningHour: Long = 8L,
    val morningMinute: Long = 0L,
    val randomQuotesEnabled: Boolean = true,
    val activeThemeAnime: String = AnimeSeries.SAKURA.key,
    val selectedVisualizerProfileId: String = "preset_sakura",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class QuoteHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quoteText: String = "",
    val animeKey: String = AnimeSeries.SAKURA.key,
    val characterName: String = "",
    val moodTag: String = "Esperanza",
    val imageUrl: String = "",
    val imageSource: String = "",
    val favorite: Boolean = false,
    val sourceType: String = "MANUAL",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class HabitItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val animeTag: String = AnimeSeries.SAKURA.key,
    val completedDates: List<String> = emptyList(),
    val currentStreak: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class VisualizerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val baseAnimeKey: String = AnimeSeries.SAKURA.key,
    val particleStyle: String = "PETALS",
    val waveformStyle: String = "ORBITAL",
    val ambientSound: String = "NIGHT_RAIN",
    val sensitivity: Double = 1.15,
    val primaryHex: String = "#7DD3FC",
    val secondaryHex: String = "#F9A8D4",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FriendLink(
    @DocumentId val id: String = "",
    val userId: String = "",
    val friendUid: String = "",
    val friendCode: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val friendCode: String = "",
    val selectedAvatarId: String = "sakura",
    val profilePhotoBase64: String = "",
    val activeThemeAnime: String = AnimeSeries.SAKURA.key,
    val activityLevel: Long = 1L,
    val totalFocusMinutes: Long = 0L,
    val climberBestMeters: Long = 0L,
    val dailyChallengeScore: Long = 0L,
    val dailyChallengeDate: String = "",
    val quotesCount: Long = 0L,
    val favoritesCount: Long = 0L,
    val habitsCompletedTotal: Long = 0L,
    val bestHabitStreak: Long = 0L,
    val dailyChallengesCompleted: Long = 0L,
    val duoClimberWins: Long = 0L,
    val duoMusicWins: Long = 0L,
    val friendsCount: Long = 0L,
    val maxEmotionalLevel: Long = 1L,
    val medals: List<String> = emptyList(),
    val unlockedAvatars: List<String> = listOf("sakura"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromDisplayName: String = "",
    val fromFriendCode: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class GameRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val gameType: String = "CLIMBER",
    val status: String = "waiting",
    val hostUid: String = "",
    val hostName: String = "",
    val hostAvatarId: String = "sakura",
    val hostProgress: Long = 0L,
    val hostReady: Boolean = true,
    val guestUid: String = "",
    val guestName: String = "",
    val guestAvatarId: String = "",
    val guestProgress: Long = 0L,
    val guestReady: Boolean = false,
    val targetGoal: Long = 500L,
    val sharedRewardGranted: Boolean = false,
    val invitedUid: String = "",
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FriendChat(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val goodVibePoints: Long = 0L,
    val emotionalLevel: Long = 1L,
    val totalMessages: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FriendChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val vibeBoost: Long = 25L,
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)
