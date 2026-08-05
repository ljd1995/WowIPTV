package com.dream.wowiptv.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test

data class FakeItem(val name: String, val date: String?)

class SortModeTest {

    private val items = listOf(
        FakeItem("Movie B", "2024-03-01 10:00:00"),
        FakeItem("movie a", "2024-05-02 10:00:00"),
        FakeItem("Movie C", null)
    )

    @Test
    fun `AZ sorts by name ignoring case`() {
        val sorted = applySort(items, SortMode.AZ, { it.name }, { it.date })
        assertEquals(listOf("movie a", "Movie B", "Movie C"), sorted.map { it.name })
    }

    @Test
    fun `RECENT sorts by date descending with nulls last`() {
        val sorted = applySort(items, SortMode.RECENT, { it.name }, { it.date })
        assertEquals(listOf("movie a", "Movie B", "Movie C"), sorted.map { it.name })
    }

    @Test
    fun `empty list stays empty`() {
        val sorted = applySort(emptyList<FakeItem>(), SortMode.AZ, { it.name }, { it.date })
        assertEquals(0, sorted.size)
    }
}
