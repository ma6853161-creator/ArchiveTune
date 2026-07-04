/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime

/**
 * A user-created, permanent snapshot of a playback queue (similar to YouTube Music's
 * "Save queue" feature). Unlike [moe.rukamori.archivetune.models.PersistQueue] (which is only
 * used to restore playback across app restarts and is overwritten continuously), rows in this
 * table are only created, renamed or removed by explicit user action.
 *
 * The queue contents (song order + metadata), the playing index and the playback position are
 * serialized into [queueData] (a serialized [moe.rukamori.archivetune.models.PersistQueue]) so
 * that queues containing songs that are not part of the user's library (e.g. songs coming from a
 * YouTube radio/mix) can still be restored faithfully. See
 * `moe.rukamori.archivetune.extensions.SavedQueueExt` for the conversion helpers.
 */
@Immutable
@Entity(tableName = "saved_queue")
data class SavedQueueEntity(
    @PrimaryKey val id: String = generateSavedQueueId(),
    val name: String,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val lastUpdateTime: LocalDateTime? = LocalDateTime.now(),
    val songCount: Int = 0,
    val thumbnailUrl: String? = null,
    val repeatMode: Int = 0,
    val shuffleModeEnabled: Boolean = false,
    val queueData: ByteArray,
) {
    companion object {
        fun generateSavedQueueId() = "SVQ" + RandomStringUtils.insecure().next(8, true, false)
    }

    // Room ignores equals()/hashCode(), these only exist to keep the data class contract sound
    // for a class that carries a ByteArray property.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedQueueEntity) return false
        return id == other.id &&
            name == other.name &&
            createdAt == other.createdAt &&
            lastUpdateTime == other.lastUpdateTime &&
            songCount == other.songCount &&
            thumbnailUrl == other.thumbnailUrl &&
            repeatMode == other.repeatMode &&
            shuffleModeEnabled == other.shuffleModeEnabled &&
            queueData.contentEquals(other.queueData)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        result = 31 * result + (lastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + songCount
        result = 31 * result + (thumbnailUrl?.hashCode() ?: 0)
        result = 31 * result + repeatMode
        result = 31 * result + shuffleModeEnabled.hashCode()
        result = 31 * result + queueData.contentHashCode()
        return result
    }
}
