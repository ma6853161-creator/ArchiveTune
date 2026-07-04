/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.PlaylistEntity
import moe.rukamori.archivetune.db.entities.PlaylistSongMap
import moe.rukamori.archivetune.db.entities.SavedQueueEntity
import moe.rukamori.archivetune.extensions.toPersistQueue
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SavedQueuesViewModel
    @Inject
    constructor(
        private val database: MusicDatabase,
    ) : ViewModel() {
        val savedQueues =
            database.savedQueues().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList(),
            )

        fun rename(
            savedQueue: SavedQueueEntity,
            newName: String,
        ) {
            val trimmed = newName.trim()
            if (trimmed.isEmpty() || trimmed == savedQueue.name) return
            viewModelScope.launch(Dispatchers.IO) {
                database.query {
                    renameSavedQueue(savedQueue.id, trimmed)
                }
            }
        }

        fun delete(savedQueue: SavedQueueEntity) {
            viewModelScope.launch(Dispatchers.IO) {
                database.query {
                    deleteSavedQueue(savedQueue.id)
                }
            }
        }

        /**
         * Saves a copy of [savedQueue] as a regular, editable playlist that the user can manage
         * from the Library the same way as any other playlist ("Save Queue as Playlist").
         */
        fun saveAsPlaylist(
            savedQueue: SavedQueueEntity,
            playlistName: String,
            onComplete: (success: Boolean) -> Unit = {},
        ) {
            val trimmedName = playlistName.trim()
            if (trimmedName.isEmpty()) {
                onComplete(false)
                return
            }
            viewModelScope.launch(Dispatchers.IO) {
                val persistQueue = savedQueue.toPersistQueue()
                if (persistQueue == null || persistQueue.items.isEmpty()) {
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }

                val playlist =
                    PlaylistEntity(
                        name = trimmedName,
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true,
                    )

                database.withTransaction {
                    insert(playlist)
                    persistQueue.items.forEachIndexed { index, song ->
                        insert(song)
                        insert(
                            PlaylistSongMap(
                                playlistId = playlist.id,
                                songId = song.id,
                                position = index,
                                setVideoId = song.setVideoId,
                            ),
                        )
                    }
                }
                withContext(Dispatchers.Main) { onComplete(true) }
            }
        }
    }
