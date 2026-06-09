package com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.model.domain.toTorrentItem
import com.github.livingwithhippos.unchained.data.model.domain.toUploadedTorrent
import com.github.livingwithhippos.unchained.data.repository.ProviderManager
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.Node
import com.github.livingwithhippos.unchained.utilities.beforeSelectionStatusList
import com.github.livingwithhippos.unchained.utilities.extension.cancelIfActive
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class TorrentProcessingViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val providerManager: ProviderManager,
) : ViewModel() {

    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val torrentLiveData = MutableLiveData<Event<TorrentEvent>>()
    val structureLiveData = MutableLiveData<Event<Node<TorrentFileItem>>>()

    private var job = Job()

    fun fetchAddedMagnet(magnet: String) {
        viewModelScope.launch {
            val repository = providerManager.getRepository()
            when (val addedMagnet = repository.addMagnet(magnet)) {
                is EitherResult.Failure -> {
                    Timber.e("Error adding magnet: ${addedMagnet.failure}")
                    networkExceptionLiveData.postEvent(addedMagnet.failure)
                }
                is EitherResult.Success -> {
                    setTorrentID(addedMagnet.success.id)
                    torrentLiveData.postEvent(
                        TorrentEvent.Uploaded(addedMagnet.success.toUploadedTorrent())
                    )
                }
            }
        }
    }

    fun fetchTorrentDetails(torrentID: String) {

        setTorrentID(torrentID)

        viewModelScope.launch {
            val torrentData: TorrentItem? =
                providerManager.getRepository().getTransferInfo(torrentID)?.toTorrentItem()
            // todo: replace using either
            if (torrentData != null) {
                setTorrentDetails(torrentData)
                torrentLiveData.postEvent(TorrentEvent.TorrentInfo(torrentData))
            } else {
                Timber.e("Retrieved torrent info were null for id $torrentID")
            }
        }
    }

    private fun setTorrentDetails(item: TorrentItem) {
        savedStateHandle[KEY_CURRENT_TORRENT] = item
    }

    fun getTorrentID(): String? {
        return savedStateHandle[KEY_CURRENT_TORRENT_ID]
    }

    private fun setTorrentID(id: String) {
        savedStateHandle[KEY_CURRENT_TORRENT_ID] = id
    }

    /** Whether the active provider requires picking files before the transfer starts. */
    fun requiresFileSelection(): Boolean =
        providerManager.getRepository().supportsFileSelection()

    fun updateTorrentStructure() {
        torrentLiveData.postEvent(TorrentEvent.SelectionUpdated)
    }

    fun startSelectionLoop(files: String = "all") {

        val id = getTorrentID()

        if (id == null) {
            Timber.e("Torrent files selection requested but torrent id was not ready")
            return
        }

        job.cancelIfActive()
        job = Job()

        val scope = CoroutineScope(job + Dispatchers.IO)

        scope.launch {
            val repository = providerManager.getRepository()
            // providers without a file selection step (e.g. Premiumize) start the transfer
            // immediately, so the selection call can be skipped
            var selected = !repository.supportsFileSelection()
            // / maybe job.isActive?
            while (isActive) {
                if (!selected) {
                    when (val selectResponse = repository.selectFiles(id, files)) {
                        is EitherResult.Failure -> {
                            if (selectResponse.failure is EmptyBodyError) {
                                Timber.d(
                                    "Select torrent files success returned ${(selectResponse.failure as EmptyBodyError).returnCode}"
                                )
                                selected = true
                            } else {
                                Timber.e(
                                    "Exception during torrent files selection call: ${selectResponse.failure}"
                                )
                            }
                        }
                        is EitherResult.Success -> {
                            Timber.d("Select torrent files success")
                            selected = true
                        }
                    }
                }

                if (selected) {
                    val torrentItem: TorrentItem? =
                        repository.getTransferInfo(id)?.toTorrentItem()
                    if (torrentItem != null) {
                        if (!beforeSelectionStatusList.contains(torrentItem.status)) {
                            job.cancelIfActive()
                            torrentLiveData.postEvent(TorrentEvent.FilesSelected(torrentItem))
                        }
                    }
                }
                delay(1500.milliseconds)
            }
        }
    }

    fun triggerTorrentEvent(event: TorrentEvent) {
        torrentLiveData.postEvent(event)
    }

    fun fetchUploadedTorrent(binaryTorrent: ByteArray) {
        viewModelScope.launch {
            val repository = providerManager.getRepository()
            when (val uploadedTorrent = repository.addTorrent(binaryTorrent)) {
                is EitherResult.Failure -> {
                    networkExceptionLiveData.postEvent(uploadedTorrent.failure)
                    torrentLiveData.postEvent(TorrentEvent.DownloadedFileFailure)
                }
                is EitherResult.Success -> {
                    fetchTorrentDetails(uploadedTorrent.success.id)
                }
            }
        }
    }

    companion object {
        const val KEY_CURRENT_TORRENT = "current_torrent_key"
        const val KEY_CURRENT_TORRENT_ID = "current_torrent_id_key"
    }
}

sealed class TorrentEvent {
    data class Uploaded(val torrent: UploadedTorrent) : TorrentEvent()

    data class TorrentInfo(val item: TorrentItem) : TorrentEvent()

    data class FilesSelected(val torrent: TorrentItem) : TorrentEvent()

    data object SelectionUpdated : TorrentEvent()

    data object DownloadAll : TorrentEvent()

    data class DownloadSelection(val filesNumber: Int) : TorrentEvent()

    data object DownloadedFileSuccess : TorrentEvent()

    data object DownloadedFileFailure : TorrentEvent()

    data class DownloadedFileProgress(val progress: Int) : TorrentEvent()
}
