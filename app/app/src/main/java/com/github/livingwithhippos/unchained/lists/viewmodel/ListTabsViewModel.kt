package com.github.livingwithhippos.unchained.lists.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.liveData
import androidx.paging.map
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.domain.toDownloadItem
import com.github.livingwithhippos.unchained.data.model.domain.toTorrentItem
import com.github.livingwithhippos.unchained.data.repository.ProviderManager
import com.github.livingwithhippos.unchained.lists.model.UnchainedDownloadPagingSource
import com.github.livingwithhippos.unchained.lists.model.UnchainedTransferPagingSource
import com.github.livingwithhippos.unchained.utilities.DOWNLOADS_TAB
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass. It offers LiveData to be observed to populate lists with paging support.
 * The lists are loaded through the active [com.github.livingwithhippos.unchained.data.repository.DebridRepository]
 * so they work with any debrid provider.
 */
@HiltViewModel
class ListTabsViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferences: SharedPreferences,
    private val providerManager: ProviderManager,
) : ViewModel() {

    // stores the last query value
    private val queryLiveData = MutableLiveData<String>()

    init {
        // reload the lists when the user switches debrid provider, avoiding stale
        // cross-provider data
        viewModelScope.launch {
            providerManager.providerChanges.collect {
                queryLiveData.postValue(queryLiveData.value ?: "")
            }
        }
    }

    // items are filtered returning only if their names contain the query
    val downloadsLiveData: LiveData<PagingData<DownloadItem>> =
        queryLiveData.switchMap { query: String ->
            val size = getPagingSize()
            val initialSize = max(size, INITIAL_LOAD)
            Pager(PagingConfig(pageSize = size, initialLoadSize = initialSize)) {
                    UnchainedDownloadPagingSource(providerManager.getRepository(), query)
                }
                .liveData
                .map { pagingData -> pagingData.map { it.toDownloadItem() } }
                .cachedIn(viewModelScope)
        }

    val torrentsLiveData: LiveData<PagingData<TorrentItem>> =
        queryLiveData.switchMap { query: String ->
            val size = getPagingSize()
            val initialSize = max(size, INITIAL_LOAD)
            Pager(PagingConfig(pageSize = size, initialLoadSize = initialSize)) {
                    UnchainedTransferPagingSource(providerManager.getRepository(), query)
                }
                .liveData
                .map { pagingData -> pagingData.map { it.toTorrentItem() } }
                .cachedIn(viewModelScope)
        }

    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()

    val downloadItemLiveData = MutableLiveData<Event<List<DownloadItem>>>()

    val deletedTorrentLiveData = MutableLiveData<Event<Int>>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()

    val eventLiveData = MutableLiveData<Event<ListEvent>>()

    /**
     * Un restrict a torrent and move it to the download section
     *
     * @param torrent
     */
    fun unrestrictTorrent(torrent: TorrentItem) {
        viewModelScope.launch {
            val repository = providerManager.getRepository()
            val values = mutableListOf<DownloadItem>()
            val errors = mutableListOf<UnchainedNetworkException>()
            torrent.links.forEach { link ->
                when (val result = repository.unrestrictLink(link)) {
                    is EitherResult.Success -> values.add(result.success.toDownloadItem())
                    is EitherResult.Failure -> errors.add(result.failure)
                }
            }

            downloadItemLiveData.postEvent(values)
            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
        }
    }

    private fun getPagingSize(): Int {
        return min(preferences.getInt("paging_size", 50), MAX_PAGE_SIZE)
    }

    fun setSelectedTab(tabID: Int) {
        savedStateHandle[KEY_SELECTED_TAB] = tabID
    }

    fun getSelectedTab(): Int {
        return savedStateHandle[KEY_SELECTED_TAB] ?: DOWNLOADS_TAB
    }

    fun setListFilter(query: String?) {
        // Avoid updating the lists if the query hasn't changed. We don't check for cases but we
        // could
        if (queryLiveData.value != query) queryLiveData.postValue(query?.trim() ?: "")
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            val repository = providerManager.getRepository()
            deletedDownloadLiveData.postEvent(0)
            var page = 1
            val completeDownloadList =
                mutableListOf<com.github.livingwithhippos.unchained.data.model.domain.UnchainedDownload>()
            do {
                val downloads = repository.getDownloads(0, page++, 50)
                completeDownloadList.addAll(downloads)
            } while (downloads.size >= 50)

            // post a message every 10% of the deletion progress if there are more than 10 items
            val progressIndicator: Int =
                if (completeDownloadList.size / 10 < 15) 15 else completeDownloadList.size / 10

            completeDownloadList.forEachIndexed { index, item ->
                repository.deleteDownload(item.id)
                if ((index + 1) % progressIndicator == 0)
                    deletedDownloadLiveData.postEvent(index + 1)
            }

            deletedDownloadLiveData.postEvent(DOWNLOADS_DELETED_ALL)
        }
    }

    fun deleteAllTorrents() {
        viewModelScope.launch {
            val repository = providerManager.getRepository()
            do {
                val torrents = repository.getTransferList(0, 1, 50)
                torrents.forEach { repository.deleteTransfer(it.id) }
            } while (torrents.size >= 50)

            deletedTorrentLiveData.postEvent(TORRENTS_DELETED_ALL)
        }
    }

    fun deleteTorrents(torrents: List<TorrentItem>) {
        viewModelScope.launch {
            val repository = providerManager.getRepository()
            torrents.forEach { repository.deleteTransfer(it.id) }
            if (torrents.size > 1) deletedTorrentLiveData.postEvent(TORRENTS_DELETED)
            else deletedTorrentLiveData.postEvent(TORRENT_DELETED)
        }
    }

    fun downloadItems(torrents: List<TorrentItem>) {
        torrents
            .filter { it.status == "downloaded" || it.status == "ready" }
            .forEach { unrestrictTorrent(it) }
    }

    fun deleteDownloads(downloads: List<DownloadItem>) {
        viewModelScope.launch {
            val repository = providerManager.getRepository()
            downloads.forEach { repository.deleteDownload(it.id) }
            if (downloads.size > 1) deletedDownloadLiveData.postEvent(DOWNLOADS_DELETED)
            else deletedDownloadLiveData.postEvent(DOWNLOAD_DELETED)
        }
    }

    fun postEventNotice(event: ListEvent) {
        eventLiveData.postEvent(event)
    }

    companion object {
        const val KEY_SELECTED_TAB = "selected_tab_key"
        const val TORRENT_DELETED = -1
        const val TORRENTS_DELETED = -2
        const val TORRENTS_DELETED_ALL = -3
        const val TORRENT_NOT_DELETED = -4
        const val DOWNLOAD_DELETED = -1
        const val DOWNLOADS_DELETED = -2
        const val DOWNLOADS_DELETED_ALL = -3
        const val DOWNLOAD_NOT_DELETED = -4

        private const val MAX_PAGE_SIZE = 2500

        private const val INITIAL_LOAD = 100
    }
}

sealed class ListEvent {
    data class DownloadItemClick(val item: DownloadItem) : ListEvent()

    data class TorrentItemClick(val item: TorrentItem) : ListEvent()

    data class OpenTorrent(val item: TorrentItem) : ListEvent()

    data class SetTab(val tab: Int) : ListEvent()

    data object NewDownload : ListEvent()
}
