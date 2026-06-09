package com.github.livingwithhippos.unchained.authentication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.PremiumizeStore
import com.github.livingwithhippos.unchained.data.remote.premiumize.PremiumizeAccountApiHelper
import com.github.livingwithhippos.unchained.data.repository.ProviderManager
import com.github.livingwithhippos.unchained.data.model.domain.DebridProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PremiumizeAuthViewModel
@Inject
constructor(
    private val premiumizeStore: PremiumizeStore,
    private val accountApiHelper: PremiumizeAccountApiHelper,
    private val providerManager: ProviderManager,
) : ViewModel() {

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    fun loginWithApiKey(apiKey: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                premiumizeStore.setApiKey(apiKey)

                val response = accountApiHelper.getAccountInfo("Bearer $apiKey")
                if (response.isSuccessful && response.body()?.status == "success") {
                    providerManager.setActiveProvider(DebridProvider.PREMIUMIZE)
                    _authResult.value = AuthResult.Success
                } else {
                    premiumizeStore.deleteCredentials()
                    _authResult.value = AuthResult.Error("Invalid API key or account error")
                }
            } catch (e: Exception) {
                premiumizeStore.deleteCredentials()
                _authResult.value =
                    AuthResult.Error(e.message ?: "Authentication failed")
            }
        }
    }

    sealed class AuthResult {
        data object Loading : AuthResult()
        data object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
}
