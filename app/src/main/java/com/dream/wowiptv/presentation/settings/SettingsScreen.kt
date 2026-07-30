package com.dream.wowiptv.presentation.settings

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dream.wowiptv.domain.model.XtreamSource
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme
import com.dream.wowiptv.presentation.common.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddSource: () -> Unit,
    onEditSource: (Long) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val sourcesState by viewModel.sources.collectAsStateWithLifecycle()
    val activeSourceId by viewModel.activeSourceId.collectAsStateWithLifecycle()
    val syncingIds by viewModel.syncingIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var prevSyncingIds by remember { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(syncingIds) {
        val newlyCompleted = prevSyncingIds - syncingIds
        if (newlyCompleted.isNotEmpty() && prevSyncingIds.isNotEmpty()) {
            Toast.makeText(context, "同步完成", Toast.LENGTH_SHORT).show()
        }
        prevSyncingIds = syncingIds
    }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设置") },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddSource,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加源")
                }
            },
            containerColor = Color(0xFF1E1E1E)
        ) { innerPadding ->
            when (val state = sourcesState) {
                is UiState.Loading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))
                is UiState.Error -> ErrorView(
                    message = state.message,
                    onRetry = {},
                    modifier = Modifier.padding(innerPadding)
                )
                is UiState.Empty -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无源，点击右下角添加",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFCCCCCC)
                    )
                }
                is UiState.Success -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.data, key = { it.id }) { source ->
                        SourceItem(
                            source = source,
                            isActive = source.id == activeSourceId,
                            isSyncing = source.id in syncingIds,
                            onEdit = { onEditSource(source.id) },
                            onSync = { viewModel.syncSource(source.id) },
                            onDelete = { viewModel.deleteSource(source.id) },
                            onSwitch = { viewModel.switchSource(source.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceItem(
    source: XtreamSource,
    isActive: Boolean,
    isSyncing: Boolean,
    onEdit: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by animateFloatAsState(
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2C)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isActive) SuccessGreen else Color(0xFF444444))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${source.serverUrl}:${source.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCCCCCC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCCCCCC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isActive) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSwitch) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "设为默认",
                        tint = Color(0xFFCCCCCC)
                    )
                }
            }
            IconButton(onClick = onSync, enabled = !isSyncing) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "同步",
                    tint = if (isSyncing) Color(0xFF888888) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.then(
                        if (isSyncing) Modifier.rotate(rotation) else Modifier
                    )
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
