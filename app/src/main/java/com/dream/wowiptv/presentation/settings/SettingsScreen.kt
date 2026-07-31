package com.dream.wowiptv.presentation.settings

import android.content.Context
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddSource: () -> Unit,
    onEditSource: (Long) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sourcesState by viewModel.sources.collectAsStateWithLifecycle()
    val activeSourceId by viewModel.activeSourceId.collectAsStateWithLifecycle()
    val syncingIds by viewModel.syncingIds.collectAsStateWithLifecycle()
    val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
    val prevSyncingIdsState = remember { mutableStateOf<Set<Long>>(emptySet()) }
    val currentSyncingIds by rememberUpdatedState(syncingIds)

    LaunchedEffect(currentSyncingIds) {
        val newlyCompleted = prevSyncingIdsState.value - currentSyncingIds
        if (newlyCompleted.isNotEmpty() && prevSyncingIdsState.value.isNotEmpty()) {
            Toast.makeText(context, "同步完成", Toast.LENGTH_SHORT).show()
        }
        prevSyncingIdsState.value = currentSyncingIds
    }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设置") },
                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF1E1E1E)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                UserInfoCard(userInfo = userInfo)
                Spacer(modifier = Modifier.height(12.dp))
                SourceListCard(
                    sources = sourcesState,
                    activeSourceId = activeSourceId,
                    syncingIds = syncingIds,
                    onEdit = onEditSource,
                    onSync = { viewModel.syncSource(it) },
                    onDelete = { viewModel.deleteSource(it) },
                    onSwitch = { viewModel.switchSource(it) },
                    onAddSource = onAddSource
                )
                Spacer(modifier = Modifier.height(12.dp))
                AboutCard(versionName = viewModel.versionName)
            }
        }
    }
}

@Composable
private fun UserInfoCard(userInfo: com.dream.wowiptv.domain.model.UserInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = if (userInfo != null)
                            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFA855F7))
                        else
                            listOf(Color(0xFF444444), Color(0xFF333333))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (userInfo != null) Color.White else Color(0xFF888888),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (userInfo != null) "VIP 会员" else "源未连接",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (userInfo != null) Color.White else Color(0xFF888888),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    if (userInfo != null) {
                        Text(
                            text = userInfo.username ?: "未知用户",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (userInfo != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "到期 ${formatExpDate(userInfo.expDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "最大连接 ${userInfo.maxConnections ?: "1"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatExpDate(dateStr: String?): String {
    if (dateStr == null || dateStr.isBlank()) return "N/A"
    val timestamp = dateStr.toLongOrNull()
    if (timestamp != null) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }
        return "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }
    return dateStr.take(10)
}

@Composable
private fun SourceListCard(
    sources: UiState<List<XtreamSource>>,
    activeSourceId: Long?,
    syncingIds: Set<Long>,
    onEdit: (Long) -> Unit,
    onSync: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onSwitch: (Long) -> Unit,
    onAddSource: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "源列表",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                IconButton(onClick = onAddSource) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加源",
                        tint = Color(0xFF6366F1)
                    )
                }
            }
            when (val state = sources) {
                is UiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.padding(16.dp))
                }
                is UiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = {},
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is UiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无源",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF888888)
                        )
                    }
                }
                is UiState.Success -> {
                    Column {
                        state.data.forEach { source ->
                            val isSrcActive = source.id == activeSourceId
                            SourceItem(
                                source = source,
                                isActive = isSrcActive,
                                isSyncing = source.id in syncingIds,
                                onEdit = { onEdit(source.id) },
                                onSync = { onSync(source.id) },
                                onDelete = { onDelete(source.id) },
                                onSwitch = if (!isSrcActive) { { onSwitch(source.id) } } else { null }
                            )
                        }
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
    onSwitch: (() -> Unit)?
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "sync")
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp)
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
                    .background(if (isActive) SuccessGreen else Color(0xFF444444), RoundedCornerShape(6.dp))
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
            if (!isActive && onSwitch != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSwitch!!) {
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
                    tint = if (isSyncing) Color(0xFF888888) else Color(0xFF6366F1),
                    modifier = Modifier.rotate(rotation)
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

@Composable
private fun AboutCard(versionName: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "WowIPTV V$versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF888888)
            )
        }
    }
}