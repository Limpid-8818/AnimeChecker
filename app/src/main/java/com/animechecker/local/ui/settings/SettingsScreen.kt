package com.animechecker.local.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animechecker.local.ui.LocalAppContainer
import com.animechecker.local.ui.Strings
import com.animechecker.local.ui.appViewModel
import com.animechecker.local.ui.components.AppTopAppBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val refreshTimeFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm")

/**
 * 设置：翻译服务配置、测试连接、队列状态与重试，以及数据与 APP 信息。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val viewModel: SettingsViewModel = appViewModel { SettingsViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle(initialValue = 0)
    val failed by viewModel.failedCount.collectAsStateWithLifecycle(initialValue = 0)
    val lastRefreshAt by viewModel.lastRefreshAt.collectAsStateWithLifecycle(initialValue = 0L)
    val versionName = LocalAppContainer.current.versionName

    Scaffold(
        topBar = { AppTopAppBar(title = Strings.TITLE_SETTINGS, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(Strings.LLM_SECTION, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::updateBaseUrl,
                label = { Text(Strings.LLM_BASE_URL_LABEL) },
                supportingText = { Text(Strings.LLM_BASE_URL_SUPPORT) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::updateApiKey,
                label = { Text(Strings.LLM_API_KEY_LABEL) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.model,
                onValueChange = viewModel::updateModel,
                label = { Text(Strings.LLM_MODEL_LABEL) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = viewModel::save) { Text(Strings.LLM_SAVE) }
                OutlinedButton(onClick = viewModel::test, enabled = !state.testing) {
                    Text(if (state.testing) Strings.LLM_TESTING else Strings.LLM_TEST)
                }
            }
            state.message?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.messageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!state.configured) {
                Text(
                    Strings.LLM_UNCONFIGURED_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    Strings.TRANSLATE_STATUS_FMT.format(pending, failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = viewModel::retryFailed,
                    enabled = !state.busy && failed > 0,
                ) {
                    Text(Strings.TRANSLATE_RETRY)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(Strings.DATA_SECTION, style = MaterialTheme.typography.titleMedium)
            val lastRefresh = if (lastRefreshAt > 0) {
                refreshTimeFormat.format(
                    Instant.ofEpochMilli(lastRefreshAt).atZone(ZoneId.of("Asia/Shanghai")),
                )
            } else {
                Strings.LAST_REFRESH_NEVER
            }
            Text(
                Strings.LAST_REFRESH_FMT.format(lastRefresh),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Strings.DATA_CREDIT,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Strings.STORAGE_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
            Text(Strings.ABOUT_SECTION, style = MaterialTheme.typography.titleMedium)
            Text(
                Strings.APP_NAME,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                Strings.VERSION_FMT.format(versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Strings.ABOUT_NETWORK_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
