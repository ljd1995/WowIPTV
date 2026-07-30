package com.dream.wowiptv.presentation.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dream.wowiptv.domain.model.VodCategory
import com.dream.wowiptv.domain.model.VodStream
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel = hiltViewModel(),
    onMovieClick: (Int) -> Unit
) {
    val categoriesState by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val streamsState by viewModel.streams.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(streamsState) {
        if (streamsState !is UiState.Loading) {
            isRefreshing = false
        }
    }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("搜索", color = Color(0xFF999999), fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(14.dp))
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFF444444),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            if (categoriesState is UiState.Success) {
                val cats = (categoriesState as UiState.Success<List<VodCategory>>).data
                if (cats.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { viewModel.selectCategory(null); viewModel.setSearchQuery("") },
                                label = { Text("全部 (${categoryCounts.values.sum()})") }
                            )
                        }
                        items(cats, key = { it.id }) { category ->
                            FilterChip(
                                selected = selectedCategoryId == category.id,
                                onClick = { viewModel.selectCategory(category.id); viewModel.setSearchQuery("") },
                                label = { Text("${category.name} (${categoryCounts[category.id] ?: 0})") }
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                val displayData = if (searchQuery.isNotBlank()) {
                    val s = streamsState
                    if (s is UiState.Success) {
                        UiState.Success(s.data.filter { it.name.contains(searchQuery, ignoreCase = true) }) as UiState<List<VodStream>>
                    } else {
                        s
                    }
                } else {
                    streamsState
                }
                when (val streams = displayData) {
                    is UiState.Loading -> {
                        if (!isRefreshing) {
                            LoadingIndicator()
                        }
                    }
                    is UiState.Error -> {
                        ErrorView(
                            message = streams.message,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                    is UiState.Empty -> {
                        Text(
                            text = "暂无电影",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                        )
                    }
                    is UiState.Success -> {
                        if (streams.data.isEmpty()) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "未找到匹配的电影" else "暂无电影",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                            )
                        } else {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    isRefreshing = true
                                    viewModel.refresh()
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    items(streams.data, key = { it.id }) { stream ->
                                        MoviePoster(
                                            stream = stream,
                                            onClick = { onMovieClick(stream.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoviePoster(
    stream: VodStream,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            AsyncImage(
                model = stream.icon,
                contentDescription = stream.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stream.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
