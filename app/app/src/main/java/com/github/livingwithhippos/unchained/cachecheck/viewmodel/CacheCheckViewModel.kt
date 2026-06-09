package com.github.livingwithhippos.unchained.cachecheck.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.domain.CacheCheckResult
import com.github.livingwithhippos.unchained.data.repository.ProviderManager
import com.github.livingwithhippos.unchained.utilities.HASH_PATTERN
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass checking torrent hashes against the active provider's cache. Only
 * available for providers where
 * [com.github.livingwithhippos.unchained.data.repository.DebridRepository.supportsCacheCheck] is
 * true.
 */
@HiltViewModel
class CacheCheckViewModel
@Inject
constructor(
    private val providerManager: ProviderManager,
) : ViewModel() {

    private val _state = MutableLiveData<CacheCheckState>()
    val state: LiveData<CacheCheckState> = _state

    private val magnetRegex = Regex(MAGNET_PATTERN, RegexOption.IGNORE_CASE)
    private val hashRegex = Regex(HASH_PATTERN)

    fun supportsCacheCheck(): Boolean =
        providerManager.getRepository().supportsCacheCheck()

    /**
     * Parses the input for magnet links or bare torrent hashes and checks them against the
     * provider cache.
     */
    fun checkCache(input: String) {
        val hashes = extractHashes(input)
        if (hashes.isEmpty()) {
            _state.value = CacheCheckState.NoHashes
            return
        }
        _state.value = CacheCheckState.Loading
        viewModelScope.launch {
            val results = providerManager.getRepository().checkCache(hashes)
            _state.value = CacheCheckState.Results(results)
        }
    }

    private fun extractHashes(input: String): List<String> {
        val fromMagnets =
            magnetRegex.findAll(input).map { it.groupValues[1] }.toList()
        // strip the magnets so their hashes are not parsed twice
        val leftover = magnetRegex.replace(input, " ")
        val bareHashes =
            leftover
                .split(Regex("\\s+"))
                .filter { hashRegex.matches(it) }
        return (fromMagnets + bareHashes).map { it.lowercase() }.distinct()
    }
}

sealed class CacheCheckState {
    data object Loading : CacheCheckState()

    data object NoHashes : CacheCheckState()

    data class Results(val results: List<CacheCheckResult>) : CacheCheckState()
}
