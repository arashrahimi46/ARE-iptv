package com.arashrahimi46.iptv.ui.browse

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource

/**
 * Shared Paging 3 configuration + builder for the browse screens (Live/Movies/Series).
 * `enablePlaceholders = false` -- callers null-guard `items[index]`; pages are small so
 * only a bounded window of a possibly-huge catalog (300k+ rows) is ever in memory.
 */
fun <T : Any> browsePager(pagingSourceFactory: () -> PagingSource<Int, T>): Pager<Int, T> =
    Pager(
        config = PagingConfig(pageSize = 60, prefetchDistance = 60, enablePlaceholders = false),
        pagingSourceFactory = pagingSourceFactory,
    )
