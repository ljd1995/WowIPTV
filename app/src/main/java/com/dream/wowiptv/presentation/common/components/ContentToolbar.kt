package com.dream.wowiptv.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dream.wowiptv.R
import com.dream.wowiptv.presentation.common.SortMode
import com.dream.wowiptv.presentation.common.theme.LocalAccentPalette

@Composable
fun <T> ContentToolbar(
    categories: List<T>,
    categoryId: (T) -> Int,
    categoryName: (T) -> String,
    selectedCategoryId: Int?,
    categoryCounts: Map<Int, Int>,
    lockedCategoryIds: Set<Int>,
    unlockedCategoryIds: Set<Int>,
    allLabel: String,
    onCategorySelected: (Int?) -> Unit,
    sortMode: SortMode,
    onSortModeChange: (SortMode) -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentPalette.current
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            TextButton(onClick = { categoryMenuOpen = true }) {
                Text(stringResource(R.string.toolbar_category), color = Color.White, fontSize = 14.sp)
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(allLabel) },
                    onClick = {
                        onCategorySelected(null)
                        categoryMenuOpen = false
                    }
                )
                categories.forEach { cat ->
                    val id = categoryId(cat)
                    val isLocked = id in lockedCategoryIds
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (id in unlockedCategoryIds && isLocked) {
                                    Icon(
                                        imageVector = Icons.Filled.LockOpen,
                                        contentDescription = stringResource(R.string.common_unlocked),
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = stringResource(R.string.common_locked),
                                        tint = Color(0xFFE6B34C),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("${categoryName(cat)} (${categoryCounts[id] ?: 0})")
                            }
                        },
                        onClick = {
                            onCategorySelected(id)
                            categoryMenuOpen = false
                        }
                    )
                }
            }
        }

        Box {
            TextButton(onClick = { sortMenuOpen = true }) {
                Text(stringResource(R.string.toolbar_sort), color = Color.White, fontSize = 14.sp)
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                SortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(stringResource(mode.labelRes)) },
                        onClick = {
                            onSortModeChange(mode)
                            sortMenuOpen = false
                        }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val options = listOf(6 to R.string.poster_small, 5 to R.string.poster_medium, 4 to R.string.poster_large)
            options.forEach { (columns, labelRes) ->
                val selected = gridColumns == columns
                IconButton(
                    onClick = { onGridColumnsChange(columns) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (selected) accent.primary.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = stringResource(labelRes),
                        color = if (selected) accent.vibrant else Color(0xFF8A8A93),
                        fontSize = 13.sp
                    )
                }
            }
        }

        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f)
        )
    }
}
