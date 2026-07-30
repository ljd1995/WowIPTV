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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.dream.wowiptv.data.local.entity.WatchProgressEntity
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onMovieClick: (Int) -> Unit,
    onSeriesClick: (Int) -> Unit,
    onLiveClick: (Int, String) -> Unit,
    onViewAllFavorites: () -> Unit,
    onViewAllRecent: () -> Unit,
    onViewAllHistory: () -> Unit
) {
    val data by viewModel.data.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

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

                if (data.continueWatching.isNotEmpty()) {
                    item {
                        Column {
                            SectionHeader(title = "继续观看", onViewAll = onViewAllHistory)
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(data.continueWatching, key = { it.contentId }) { wp ->
                                    ContinueCard(
                                        name = decodeName(wp.name),
                                        icon = wp.icon,
                                        position = wp.position,
                                        duration = wp.duration,
                                        onClick = {
                                            val idStr = wp.contentId.removePrefix("vod_").removePrefix("series_").removePrefix("live_")
                                            when (wp.contentType) {
                                                "vod" -> idStr.toIntOrNull()?.let { onMovieClick(it) }
                                                "series" -> idStr.toIntOrNull()?.let { onSeriesClick(it) }
                                                "live" -> idStr.toIntOrNull()?.let { onLiveClick(it, wp.name) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }

                item {
                    Column {
                        SectionHeader(title = "收藏", onViewAll = onViewAllFavorites)
                        Spacer(modifier = Modifier.height(10.dp))
                        val items = data.favoriteStreams + data.favoriteMovies + data.favoriteSeries
                        if (items.isEmpty()) {
                            Text("暂无收藏", color = Color(0xFF888888), modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(items.take(10), key = { it.hashCode() }) { item ->
                                    when (item) {
                                        is FavoriteStreamEntity -> FavCard(name = item.name, icon = item.iconUrl, onClick = { onLiveClick(item.streamId, item.name) })
                                        is FavoriteVodEntity -> FavCard(name = item.name, icon = item.icon, onClick = {
                                            if (item.type == "movie") onMovieClick(item.vodId) else onSeriesClick(item.vodId)
                                        })
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                item {
                    Column {
                        SectionHeader(title = "最近添加", onViewAll = onViewAllRecent)
                        Spacer(modifier = Modifier.height(10.dp))
                        val all = data.recentLive + data.recentMovies + data.recentSeries
                        if (all.isEmpty()) {
                            Text("暂无内容", color = Color(0xFF888888), modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(all.take(10), key = { it.hashCode() }) { item ->
                                    when (item) {
                                        is LiveStreamEntity -> RecentCard(name = item.name, icon = item.streamIcon)
                                        is VodStreamEntity -> RecentCard(name = item.name.orEmpty(), icon = item.streamIcon, onClick = { onMovieClick(item.streamId) })
                                        is SeriesEntity -> RecentCard(name = item.name.orEmpty(), icon = item.cover, onClick = { onSeriesClick(item.seriesId) })
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

private fun decodeName(raw: String): String {
    return try { java.net.URLDecoder.decode(raw, "UTF-8") } catch (_: Exception) { raw }
}

@Composable
private fun SectionHeader(title: String, onViewAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (onViewAll != null) {
            Text(
                text = "查看全部",
                color = Color(0xFF1E88E5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }
    }
}

@Composable
private fun FavCard(name: String, icon: String? = null, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.width(105.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C2C2C)).clickable(onClick = onClick)) {
        if (!icon.isNullOrEmpty()) {
            AsyncImage(model = icon, contentDescription = name,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(32.dp))
            }
        }
        Box(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(text = name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ContinueCard(name: String, icon: String? = null, position: Long = 0L, duration: Long = 0L, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.width(105.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C2C2C)).clickable(onClick = onClick)) {
        if (!icon.isNullOrEmpty()) {
            AsyncImage(model = icon, contentDescription = name,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(32.dp))
            }
        }
        Box(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(text = name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (duration > 0) {
            val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0xFF555555))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color(0xFF1E88E5)))
            }
        }
    }
}

@Composable
private fun RecentCard(name: String, icon: String? = null, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.width(105.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C2C2C)).clickable(onClick = onClick)) {
        if (!icon.isNullOrEmpty()) {
            AsyncImage(model = icon, contentDescription = name,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(32.dp))
            }
        }
        Box(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(text = name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}