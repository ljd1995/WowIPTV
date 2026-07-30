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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dream.wowiptv.data.local.entity.FavoriteStreamEntity
import com.dream.wowiptv.data.local.entity.FavoriteVodEntity
import com.dream.wowiptv.data.local.entity.LiveStreamEntity
import com.dream.wowiptv.data.local.entity.SeriesEntity
import com.dream.wowiptv.data.local.entity.VodStreamEntity
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onMovieClick: (Int) -> Unit,
    onSeriesClick: (Int) -> Unit,
    onViewAll: () -> Unit
) {
    val data by viewModel.data.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var recentTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("全部", "电影", "剧集")

    MaterialTheme(colorScheme = DarkColorScheme) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    SectionHeader(title = "收藏", onViewAll = onViewAll)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val items = data.favoriteStreams + data.favoriteMovies + data.favoriteSeries
                        if (items.isEmpty()) {
                            item { Text("暂无收藏", color = Color(0xFF888888), modifier = Modifier.padding(vertical = 16.dp)) }
                        } else {
                            items(items.take(10), key = { it.hashCode() }) { item ->
                                when (item) {
                                    is FavoriteStreamEntity -> FavStreamCard(item)
                                    is FavoriteVodEntity -> FavVodCard(item, onClick = {
                                        if (item.type == "movie") onMovieClick(item.vodId) else onSeriesClick(item.vodId)
                                    })
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(title = "最近添加", onViewAll = onViewAll)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tabs.forEachIndexed { idx, label ->
                            FilterChip(
                                selected = recentTab == idx,
                                onClick = { recentTab = idx },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                val gridItems: List<Any> = when (recentTab) {
                    1 -> data.recentMovies
                    2 -> data.recentSeries
                    else -> data.recentLive + data.recentMovies + data.recentSeries
                }
                if (gridItems.isNotEmpty()) {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().height(380.dp)
                        ) {
                            items(gridItems.take(18), key = { it.hashCode() }) { item ->
                                when (item) {
                                    is LiveStreamEntity -> RecentGridItem(icon = item.streamIcon, name = item.name)
                                    is VodStreamEntity -> RecentGridItem(icon = item.streamIcon, name = item.name.orEmpty(), onClick = { onMovieClick(item.streamId) })
                                    is SeriesEntity -> RecentGridItem(icon = item.cover, name = item.name.orEmpty(), onClick = { onSeriesClick(item.seriesId) })
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
private fun SectionHeader(title: String, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            text = "查看全部",
            color = Color(0xFF1E88E5),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onViewAll)
        )
    }
}

@Composable
private fun FavStreamCard(stream: FavoriteStreamEntity) {
    Box(modifier = Modifier.width(110.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C2C2C))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!stream.iconUrl.isNullOrEmpty()) {
                AsyncImage(model = stream.iconUrl, contentDescription = stream.name,
                    modifier = Modifier.size(110.dp, 62.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(110.dp, 62.dp).background(Color(0xFF444444)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
                }
            }
            Text(text = stream.name, color = Color(0xFFDDDDDD), fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun FavVodCard(item: FavoriteVodEntity, onClick: () -> Unit) {
    Box(modifier = Modifier.width(110.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C2C2C)).clickable(onClick = onClick)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!item.icon.isNullOrEmpty()) {
                AsyncImage(model = item.icon, contentDescription = item.name,
                    modifier = Modifier.size(110.dp, 62.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(110.dp, 62.dp).background(Color(0xFF444444)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
                }
            }
            Text(text = item.name, color = Color(0xFFDDDDDD), fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun RecentGridItem(icon: String? = null, name: String, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2C2C2C)).clickable(onClick = onClick)) {
        if (!icon.isNullOrEmpty()) {
            AsyncImage(model = icon, contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(24.dp))
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 4.dp, vertical = 3.dp)) {
            Text(text = name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}