package com.dream.wowiptv.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dream.wowiptv.data.local.entity.FavoriteStreamEntity
import com.dream.wowiptv.data.local.entity.FavoriteVodEntity
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFavoritesScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onSeriesClick: (Int) -> Unit
) {
    val data by viewModel.data.collectAsState()

    MaterialTheme(colorScheme = DarkColorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("我的收藏", color = Color.White) },
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
            val items = data.favoriteStreams + data.favoriteMovies + data.favoriteSeries
            if (items.isEmpty()) {
                Text("暂无收藏", color = Color(0xFF888888), modifier = Modifier.fillMaxSize().padding(innerPadding).wrapContentSize(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    items(items, key = { it.hashCode() }) { item ->
                        when (item) {
                            is FavoriteStreamEntity -> FavGridCell(name = item.name, icon = item.iconUrl)
                            is FavoriteVodEntity -> FavGridCell(name = item.name, icon = item.icon, onClick = {
                                if (item.type == "movie") onMovieClick(item.vodId) else onSeriesClick(item.vodId)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavGridCell(name: String, icon: String? = null, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C2C2C)).clickable(onClick = onClick)) {
        if (!icon.isNullOrEmpty()) {
            AsyncImage(model = icon, contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(32.dp))
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 4.dp, vertical = 3.dp)) {
            Text(text = name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

