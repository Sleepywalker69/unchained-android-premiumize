package com.github.livingwithhippos.unchained.cachecheck.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.cachecheck.viewmodel.CacheCheckState
import com.github.livingwithhippos.unchained.cachecheck.viewmodel.CacheCheckViewModel
import com.github.livingwithhippos.unchained.databinding.FragmentCacheCheckBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * A simple [Fragment] subclass. Lets the user paste magnet links or torrent hashes and check them
 * against the active provider's cache (Premiumize only).
 */
@AndroidEntryPoint
class CacheCheckFragment : Fragment() {

    private val viewModel: CacheCheckViewModel by viewModels()

    private var _binding: FragmentCacheCheckBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCacheCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCheckCache.setOnClickListener {
            viewModel.checkCache(binding.etHashes.text?.toString() ?: "")
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                CacheCheckState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvResults.text = ""
                }
                CacheCheckState.NoHashes -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvResults.text = getString(R.string.cache_check_no_hashes)
                }
                is CacheCheckState.Results -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.results.isEmpty()) {
                        binding.tvResults.text = getString(R.string.cache_check_no_results)
                    } else {
                        binding.tvResults.text =
                            state.results.joinToString("\n\n") { result ->
                                val status =
                                    if (result.isCached) getString(R.string.cached)
                                    else getString(R.string.not_cached)
                                val name = result.filename ?: result.hash
                                "$name\n$status"
                            }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
