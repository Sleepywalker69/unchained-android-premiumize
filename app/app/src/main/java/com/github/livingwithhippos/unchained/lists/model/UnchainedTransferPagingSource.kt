package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.model.domain.DebridProvider
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedTransfer
import com.github.livingwithhippos.unchained.data.repository.DebridRepository
import java.io.IOException
import retrofit2.HttpException

private const val TRANSFER_STARTING_PAGE_INDEX = 1

class UnchainedTransferPagingSource(
    private val repository: DebridRepository,
    private val query: String,
) : PagingSource<Int, UnchainedTransfer>() {

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, UnchainedTransfer> {
        val page = params.key ?: TRANSFER_STARTING_PAGE_INDEX

        return try {
            val response = repository.getTransferList(null, page, params.loadSize)

            val filtered =
                if (query.isBlank()) response
                else response.filter { it.name.contains(query, ignoreCase = true) }

            val isPremiumize = repository.provider == DebridProvider.PREMIUMIZE
            LoadResult.Page(
                data = filtered,
                prevKey = if (page == TRANSFER_STARTING_PAGE_INDEX) null else page - 1,
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

    override val jumpingSupported: Boolean = true

    override fun getRefreshKey(state: PagingState<Int, UnchainedTransfer>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
