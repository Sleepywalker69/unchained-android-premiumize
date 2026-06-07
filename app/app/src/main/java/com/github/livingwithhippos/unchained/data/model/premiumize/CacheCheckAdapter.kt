package com.github.livingwithhippos.unchained.data.model.premiumize

import com.github.livingwithhippos.unchained.data.model.domain.CacheCheckResult

object CacheCheckAdapter {

    fun parseResponse(
        raw: PremiumizeCacheCheckRawResponse,
        hashes: List<String>,
    ): List<CacheCheckResult> {
        val responses = raw.response ?: return emptyList()
        val filenames = raw.filename
        val filesizes = raw.filesize

        return responses.mapIndexed { index, isCached ->
            CacheCheckResult(
                hash = hashes.getOrElse(index) { "" },
                isCached = isCached,
                filename = filenames?.getOrNull(index),
                filesize = filesizes?.getOrNull(index)?.toLongOrNull(),
            )
        }
    }
}
