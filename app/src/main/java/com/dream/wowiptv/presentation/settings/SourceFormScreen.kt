package com.dream.wowiptv.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dream.wowiptv.domain.model.XtreamSource
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.LoadingIndicator

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

    key(sourceId ?: -1L) {
        SourceFormInner(
            initialSource = editingSource,
            isEditing = sourceId != null,
            onSave = { name, serverUrl, port, username, password ->
                if (sourceId != null) {
                    viewModel.updateSource(sourceId, name, serverUrl, port, username, password)
                } else {
                    viewModel.addSource(name, serverUrl, port, username, password)
                }
                onNavigateBack()
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
    onSave: (name: String, serverUrl: String, port: Int, username: String, password: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf(initialSource?.name ?: "") }
    var serverUrl by remember { mutableStateOf(initialSource?.serverUrl ?: "") }
    var port by remember { mutableStateOf(initialSource?.port?.toString() ?: "25461") }
    var username by remember { mutableStateOf(initialSource?.username ?: "") }
    var password by remember { mutableStateOf(initialSource?.password ?: "") }
    var saving by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑源" else "添加源") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
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
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("服务器地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() } },
                label = { Text("端口") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    saving = true
                    val portNum = port.toIntOrNull() ?: 25461
                    onSave(name.trim(), serverUrl.trim(), portNum, username.trim(), password.trim())
                },
                enabled = isValid && !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存")
                }
            }
        }
    }
}
