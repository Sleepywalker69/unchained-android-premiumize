package com.github.livingwithhippos.unchained.torrentdetails.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.domain.toDownloadItem
import com.github.livingwithhippos.unchained.data.model.domain.toTorrentItem
import com.github.livingwithhippos.unchained.data.repository.ProviderManager
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.endedStatusList
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
import kotlin.time.Duration.Companion.milliseconds

/** a [ViewModel] subclass. Retrieves a torrent's details */
@HiltViewModel
class TorrentDetailsViewModel
@Inject
constructor(
    private val providerManager: ProviderManager,
) : ViewModel() {

    val torrentLiveData = MutableLiveData<Event<TorrentItem?>>()
    val deletedTorrentLiveData = MutableLiveData<Event<Int>>()
    val downloadLiveData = MutableLiveData<Event<DownloadItem?>>()
    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()

    private var job = Job()

    fun getFullTorrentInfo(id: String) {
        viewModelScope.launch {
            val torrentData = providerManager.getRepository().getTransferInfo(id)
            if (torrentData != null) torrentLiveData.postEvent(torrentData.toTorrentItem())
        }
    }

    fun pollTorrentStatus(id: String) {
        // todo: test if I need to recreate a job when it is cancelled
        job.cancelIfActive()
        job = Job()

        val scope = CoroutineScope(job + Dispatchers.IO)

        scope.launch {
            // / maybe job.isActive?
            while (isActive) {
                val torrentData =
                    providerManager.getRepository().getTransferInfo(id)?.toTorrentItem()
                if (torrentData != null) torrentLiveData.postEvent(torrentData)
                if (endedStatusList.contains(torrentData?.status)) job.cancelIfActive()

                delay(2000.milliseconds)
            }
        }
    }

    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            when (val deleted = providerManager.getRepository().deleteTransfer(id)) {
                is EitherResult.Failure -> {
                    errorsLiveData.postEvent(listOf(deleted.failure))
                }
                is EitherResult.Success -> {
                    deletedTorrentLiveData.postEvent(204)
                }
            }
        }
    }

    fun downloadTorrent(torrent: TorrentItem) {
        viewModelScope.launch {
            val links = torrent.links
            if (links.isNotEmpty()) {
                val repository = providerManager.getRepository()
                val values = mutableListOf<DownloadItem>()
                val errors = mutableListOf<UnchainedNetworkException>()
                links.forEach { link ->
                    when (val result = repository.unrestrictLink(link)) {
                        is EitherResult.Success -> values.add(result.success.toDownloadItem())
                        is EitherResult.Failure -> errors.add(result.failure)
                    }
                }

                // since the torrent want to open a download details page we oen only the first link
                downloadLiveData.postEvent(values.firstOrNull())
                if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
            }
        }
    }
}
