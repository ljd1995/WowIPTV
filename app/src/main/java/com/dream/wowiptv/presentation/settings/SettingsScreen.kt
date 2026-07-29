package com.dream.wowiptv.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dream.wowiptv.domain.model.XtreamSource
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSource,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加源")
            }
        }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        onEdit = { onEditSource(source.id) },
                        onDelete = { viewModel.deleteSource(source.id) },
                        onSwitch = { viewModel.switchSource(source.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceItem(
    source: XtreamSource,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    .background(if (isActive) SuccessGreen else MaterialTheme.colorScheme.outline)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${source.serverUrl}:${source.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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


