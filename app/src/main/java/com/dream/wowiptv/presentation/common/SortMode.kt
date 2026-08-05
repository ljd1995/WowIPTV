package com.dream.wowiptv.presentation.common

import com.dream.wowiptv.R

enum class SortMode(val labelRes: Int) {
    AZ(R.string.sort_az),
    RECENT(R.string.sort_recent)
}

fun <T> applySort(
    items: List<T>,
    mode: SortMode,
    nameOf: (T) -> String,
    dateOf: (T) -> String?
): List<T> = when (mode) {
    SortMode.AZ -> items.sortedBy { nameOf(it).lowercase() }
    SortMode.RECENT -> items.sortedByDescending { dateOf(it) }
}
