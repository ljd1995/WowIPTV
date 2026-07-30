package com.dream.wowiptv.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dream.wowiptv.domain.model.XtreamSource
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme
import kotlinx.coroutines.launch
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceFormScreen(
    sourceId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val sourcesState by viewModel.sources.collectAsStateWithLifecycle()

    val editingSource = if (sourceId != null) {
        val list = (sourcesState as? UiState.Success)?.data ?: emptyList()
        list.find { it.id == sourceId }
    } else null

    if (sourceId != null && editingSource == null && sourcesState !is UiState.Empty && sourcesState !is UiState.Error) {
        LoadingIndicator(message = "加载中...")
        return
    }

    val scope = rememberCoroutineScope()
    key(sourceId ?: -1L) {
        SourceFormInner(
            initialSource = editingSource,
            isEditing = sourceId != null,
            onSave = { name, serverUrl, username, password ->
                scope.launch {
                    val (host, port) = parseServerUrl(serverUrl)
                    if (sourceId != null) {
                        viewModel.updateSource(sourceId, name, host, port, username, password)
                    } else {
                        viewModel.addSource(name, host, port, username, password)
                    }
                    onNavigateBack()
                }
            },
            onNavigateBack = onNavigateBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceFormInner(
    initialSource: XtreamSource?,
    isEditing: Boolean,
    onSave: (name: String, serverUrl: String, username: String, password: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val initialUrl = if (initialSource != null) "http://${initialSource.serverUrl}:${initialSource.port}" else ""
    var name by remember { mutableStateOf(initialSource?.name ?: "") }
    var serverUrl by remember { mutableStateOf(initialUrl) }
    var username by remember { mutableStateOf(initialSource?.username ?: "") }
    var password by remember { mutableStateOf(initialSource?.password ?: "") }
    var saving by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    val textColor = Color.White
    val labelColor = Color(0xFF999999)
    val fieldBg = Color(0xFF2C2C2C)

    MaterialTheme(colorScheme = DarkColorScheme) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑源" else "添加源", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称", color = labelColor) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = Color(0xFF555555),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    focusedLabelColor = labelColor,
                    unfocusedLabelColor = labelColor
                ),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("服务器地址", color = labelColor) },
                placeholder = { Text("http://example.com:25461", color = Color(0xFF666666)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = Color(0xFF555555),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    focusedLabelColor = labelColor,
                    unfocusedLabelColor = labelColor
                ),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名", color = labelColor) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = Color(0xFF555555),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    focusedLabelColor = labelColor,
                    unfocusedLabelColor = labelColor
                ),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码", color = labelColor) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = Color(0xFF555555),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    focusedLabelColor = labelColor,
                    unfocusedLabelColor = labelColor
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    saving = true
                    onSave(name.trim(), serverUrl.trim(), username.trim(), password.trim())
                },
                enabled = isValid && !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E88E5)
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("保存", color = Color.White)
                }
            }
        }
    }
    }
}

private fun parseServerUrl(input: String): Pair<String, Int> {
    var url = input.trim()
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "http://$url"
    }
    val uri = URI(url)
    val host = uri.host ?: ""
    val port = if (uri.port > 0) uri.port else 80
    return Pair(host, port)
}
