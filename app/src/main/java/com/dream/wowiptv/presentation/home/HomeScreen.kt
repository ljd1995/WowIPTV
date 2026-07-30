package com.dream.wowiptv.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFF8B5CF6),
                                            Color(0xFFA855F7)
                                        )
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Hello, ${data.username}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (data.expiryDate.length > 0) "VIP 到期: ${data.expiryDate}" else "VIP 未设置",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatItem(value = data.liveCount, label = "直播")
                                        StatItem(value = data.movieCount, label = "电影")
                                        StatItem(value = data.seriesCount, label = "剧集")
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (data.continueWatching.isNotEmpty()) {
                    item {
                        Column {
                            SectionHeader(title = "继续观看", onViewAll = onViewAllHistory)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(data.continueWatching, key = { it.contentId }) { wp ->
                                    val badgeLabel = when (wp.contentType) {
                                        "vod" -> "MOVIE"
                                        "series" -> "SERIES"
                                        "live" -> "LIVE"
                                        else -> ""
                                    }
                                    ContinueCard(
                                        name = decodeName(wp.name),
                                        icon = wp.icon,
                                        badge = badgeLabel,
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
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Column {
                        SectionHeader(title = "收藏", onViewAll = onViewAllFavorites)
                        Spacer(modifier = Modifier.height(16.dp))
                        val items = data.favoriteStreams + data.favoriteMovies + data.favoriteSeries
                        if (items.isEmpty()) {
                            Text("暂无收藏", color = Color(0xFF888888), modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(items.take(10), key = { it.hashCode() }) { item ->
                                    when (item) {
                                        is FavoriteStreamEntity -> FavCard(name = item.name, icon = item.iconUrl, badge = "LIVE", onClick = { onLiveClick(item.streamId, item.name) })
                                        is FavoriteVodEntity -> {
                                            val catMap = if (item.type == "movie") data.vodCategoryNames else data.seriesCategoryNames
                                            val catName = catMap[item.categoryId]
                                            val badge = if (item.type == "movie") "MOVIE" else "SERIES"
                                            FavCard(name = item.name, icon = item.icon, badge = badge, categoryName = catName, onClick = {
                                                if (item.type == "movie") onMovieClick(item.vodId) else onSeriesClick(item.vodId)
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Column {
                        SectionHeader(title = "最近添加", onViewAll = onViewAllRecent)
                        Spacer(modifier = Modifier.height(16.dp))
                        val all = data.recentLive + data.recentMovies + data.recentSeries
                        if (all.isEmpty()) {
                            Text("暂无内容", color = Color(0xFF888888), modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(all.take(10), key = { it.hashCode() }) { item ->
                                    when (item) {
                                        is LiveStreamEntity -> {
                                            val catName = data.liveCategoryNames[item.categoryId]
                                            RecentCard(name = item.name, icon = item.streamIcon, badge = "LIVE", categoryName = catName, onClick = { onLiveClick(item.streamId, item.name) })
                                        }
                                        is VodStreamEntity -> {
                                            val catName = data.vodCategoryNames[item.categoryId]
                                            RecentCard(name = item.name.orEmpty(), icon = item.streamIcon, badge = "MOVIE", categoryName = catName, onClick = { onMovieClick(item.streamId) })
                                        }
                                        is SeriesEntity -> {
                                            val catName = data.seriesCategoryNames[item.categoryId]
                                            RecentCard(name = item.name.orEmpty(), icon = item.cover, badge = "SERIES", categoryName = catName, onClick = { onSeriesClick(item.seriesId) })
                                        }
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
private fun StatItem(value: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionHeader(title: String, onViewAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        if (onViewAll != null) {
            Row(
                modifier = Modifier.clickable(onClick = onViewAll),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "查看全部",
                    color = Color(0xFF8B5CF6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "›",
                    color = Color(0xFF8B5CF6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Box(
        modifier = Modifier
            .padding(start = 4.dp, top = 4.dp)
            .background(
                color = Color(0xFFEF4444),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.White, RoundedCornerShape(3.dp))
            )
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun TypeBadge(text: String, bgColor: Color) {
    Box(
        modifier = Modifier
            .padding(start = 4.dp, top = 4.dp)
            .background(bgColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun MediaCard(name: String, icon: String? = null, badge: String? = null, categoryName: String? = null, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
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
                TypeBadge(
                    text = badge,
                    bgColor = if (badge == "LIVE") Color(0xFFEF4444) else Color(0xFF000000).copy(alpha = 0.6f)
                )
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
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
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

@Composable
private fun FavCard(name: String, icon: String? = null, badge: String? = null, categoryName: String? = null, onClick: () -> Unit = {}) {
    MediaCard(name, icon, badge, categoryName, onClick)
}

@Composable
private fun ContinueCard(name: String, icon: String? = null, badge: String? = null, categoryName: String? = null, position: Long = 0L, duration: Long = 0L, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
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
                TypeBadge(
                    text = badge,
                    bgColor = if (badge == "LIVE") Color(0xFFEF4444) else Color(0xFF000000).copy(alpha = 0.6f)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
            Column {
                if (duration > 0) {
                    val p = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color(0xFF555555).copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(p)
                                .background(Color(0xFF6366F1))
                        )
                    }
                }
                Box(
                    modifier = Modifier
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
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentCard(name: String, icon: String? = null, badge: String? = null, categoryName: String? = null, onClick: () -> Unit = {}) {
    MediaCard(name, icon, badge, categoryName, onClick)
}
