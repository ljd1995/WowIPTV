package com.dream.wowiptv.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dream.wowiptv.data.local.entity.LiveStreamEntity
import com.dream.wowiptv.data.local.entity.SeriesEntity
import com.dream.wowiptv.data.local.entity.VodStreamEntity
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllItemsScreen(
    initialTab: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onSeriesClick: (Int) -> Unit,
    onLiveClick: (Int, String) -> Unit = { _, _ -> }
) {
    val data by viewModel.data.collectAsState()
    var tab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("全部", "直播", "电影", "剧集")

    MaterialTheme(colorScheme = DarkColorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("全部内容", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
                )
            },
            containerColor = Color(0xFF1E1E1E)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tabs.forEachIndexed { idx, label ->
                            FilterChip(
                                selected = tab == idx,
                                onClick = { tab = idx },
                                label = { Text(label, fontSize = 13.sp) }
                            )
                        }
                    }
                }

                val items: List<Any> = when (tab) {
                    1 -> data.recentLive
                    2 -> data.recentMovies
                    3 -> data.recentSeries
                    else -> data.recentLive + data.recentMovies + data.recentSeries
                }

                if (items.isEmpty()) {
                    item {
                        Text("暂无内容", color = Color(0xFF888888), modifier = Modifier.padding(vertical = 32.dp))
                    }
                } else {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().height(((items.size / 3 + 1) * 180).dp)
                        ) {
                            items(items, key = { it.hashCode() }) { item ->
                                when (item) {
                                    is LiveStreamEntity -> {
                                        val catName = data.liveCategoryNames[item.categoryId]
                                        GridCell(
                                            icon = item.streamIcon,
                                            name = item.name,
                                            badge = "LIVE",
                                            categoryName = catName,
                                            onClick = { onLiveClick(item.streamId, item.name) }
                                        )
                                    }
                                    is VodStreamEntity -> {
                                        val catName = data.vodCategoryNames[item.categoryId]
                                        GridCell(
                                            icon = item.streamIcon,
                                            name = item.name.orEmpty(),
                                            badge = "MOVIE",
                                            categoryName = catName,
                                            onClick = { onMovieClick(item.streamId) }
                                        )
                                    }
                                    is SeriesEntity -> {
                                        val catName = data.seriesCategoryNames[item.categoryId]
                                        GridCell(
                                            icon = item.cover,
                                            name = item.name.orEmpty(),
                                            badge = "SERIES",
                                            categoryName = catName,
                                            onClick = { onSeriesClick(item.seriesId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun GridCell(icon: String? = null, name: String, badge: String? = null, categoryName: String? = null, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D2D3A))
            .clickable(onClick = onClick)
    ) {
        if (!icon.isNullOrEmpty()) {
            AsyncImage(
                model = icon,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color(0xFF666666).copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        if (!badge.isNullOrEmpty()) {
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 4.dp)
                        .background(Color(0xFF000000).copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (badge == "LIVE") {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(Color(0xFFEF4444), RoundedCornerShape(2.5.dp))
                            )
                        }
                        Text(
                            text = badge,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!categoryName.isNullOrEmpty()) {
                    Text(
                        text = categoryName,
                        color = Color(0xFF999999),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
