package com.plcoding.videoplayercompose

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val player: Player,
    private val metaDataReader: MetaDataReader
) : ViewModel() {

    private val videoUris = savedStateHandle.getStateFlow(VIDEO_URIS_STATE_KEY, emptyList<Uri>())

    val videoItems = videoUris
        .map { uris ->
            uris
                .map { uri ->
                    VideoItem(
                        contentUri = uri,
                        mediaItem = MediaItem.fromUri(uri),
                        name = metaDataReader
                            .getMetaDataFromUri(uri)?.fileName
                            ?: uri.toString()
                    )
                }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        player.prepare()
    }

    fun addVideoUri(uri: Uri) {
        savedStateHandle[VIDEO_URIS_STATE_KEY] = videoUris.value + uri
        player.addMediaItem(MediaItem.fromUri(uri))
    }

    fun addVideoUrl() {
        val uri = Uri.parse(SAMPLE_VIDEO_URL)
        savedStateHandle[VIDEO_URIS_STATE_KEY] = videoUris.value + uri
        player.addMediaItem(MediaItem.fromUri(uri))
    }

    fun playVideo(uri: Uri) {
        player.setMediaItem(
            videoItems.value.find { it.contentUri == uri }?.mediaItem ?: return
        )
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }

    companion object {
        const val VIDEO_URIS_STATE_KEY = "videoUris"
        const val SAMPLE_VIDEO_URL =
            "https://ia600805.us.archive.org/13/items/ATaleOfTwoKitties1942/A%20Tale%20of%20Two%20Kitties%20%281942%29.mp4"
    }
}