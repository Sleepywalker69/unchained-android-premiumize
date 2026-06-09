package com.github.livingwithhippos.unchained.authentication.view

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.PremiumizeAuthViewModel
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentPremiumizeAuthBinding
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PremiumizeAuthFragment : UnchainedFragment() {

    private val viewModel: PremiumizeAuthViewModel by viewModels()

    private var _binding: FragmentPremiumizeAuthBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPremiumizeAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLoginApiKey.setOnClickListener {
            val apiKey = binding.etApiKey.text?.toString()?.trim()
            if (apiKey.isNullOrBlank()) {
                Toast.makeText(requireContext(), R.string.premiumize_api_key, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            loginWithApiKey(apiKey)
        }

        binding.btnPasteApiKey.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pastedText = clip.getItemAt(0).text?.toString()
                if (!pastedText.isNullOrBlank()) {
                    binding.etApiKey.setText(pastedText)
                }
            }
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is PremiumizeAuthViewModel.AuthResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvStatus.visibility = View.GONE
                }
                is PremiumizeAuthViewModel.AuthResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.visibility = View.GONE
                    // moves the authentication machine to CheckCredentials, which validates the
                    // stored api key and unlocks the rest of the app
                    activityViewModel.transitionAuthenticationMachine(
                        FSMAuthenticationEvent.OnPrivateToken
                    )
                    findNavController()
                        .navigate(R.id.actionPremiumizeAuthToUser)
                }
                is PremiumizeAuthViewModel.AuthResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.visibility = View.VISIBLE
                    binding.tvStatus.text = result.message
                }
            }
        }
    }

    private fun loginWithApiKey(apiKey: String) {
        lifecycleScope.launch { viewModel.loginWithApiKey(apiKey) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
