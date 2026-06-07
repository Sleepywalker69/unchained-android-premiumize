package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.model.domain.DebridProvider
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedDownload
import com.github.livingwithhippos.unchained.data.repository.DebridRepository
import java.io.IOException
import retrofit2.HttpException

private const val DOWNLOAD_STARTING_PAGE_INDEX = 1

class UnchainedDownloadPagingSource(
    private val repository: DebridRepository,
    private val query: String,
) : PagingSource<Int, UnchainedDownload>() {

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, UnchainedDownload> {
        val page = params.key ?: DOWNLOAD_STARTING_PAGE_INDEX

        return try {
            val response = repository.getDownloads(null, page, params.loadSize)

            val filtered =
                if (query.isBlank()) response
                else response.filter { it.filename.contains(query, ignoreCase = true) }

            val isPremiumize = repository.provider == DebridProvider.PREMIUMIZE
            LoadResult.Page(
                data = filtered,
                prevKey = if (page == DOWNLOAD_STARTING_PAGE_INDEX) null else page - 1,
                nextKey =
                    if (filtered.isEmpty() || isPremiumize) null
                    else page + 1,
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UnchainedDownload>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
