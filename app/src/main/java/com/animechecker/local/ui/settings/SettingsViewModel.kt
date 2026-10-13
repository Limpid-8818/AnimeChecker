package com.animechecker.local.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animechecker.local.data.repository.AppError
import com.animechecker.local.data.repository.AppRepository
import com.animechecker.local.domain.model.LlmSettings
import com.animechecker.local.ui.Strings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 设置页状态：翻译服务配置的编辑副本 + 已保存配置是否可用。
 *
 * 配置只在进入页面时读一次，队列计数与上次刷新时间直接订阅 repository 的 Flow。
 */
data class SettingsUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    /** 已保存的配置是否可用：false 时只显示降级说明，不展示队列与重试入口。 */
    val configured: Boolean = false,
    val testing: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    /** 提示语是否为失败：成功与引导语用普通色，失败用错误色。 */
    val messageIsError: Boolean = false,
) {
    val edited: LlmSettings get() = LlmSettings(baseUrl = baseUrl, apiKey = apiKey, model = model)
}

/** 设置页动作：保存配置、测试连接、重试失败项。 */
class SettingsViewModel(private val repository: AppRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    val pendingCount: Flow<Int> = repository.translationPendingCount()

    val failedCount: Flow<Int> = repository.translationFailedCount()

    val lastRefreshAt: Flow<Long> = repository.lastRefreshAt()

    init {
        viewModelScope.launch {
            val saved = repository.llmSettings().first()
            _state.update {
                it.copy(
                    baseUrl = saved.baseUrl,
                    apiKey = saved.apiKey,
                    model = saved.model,
                    configured = saved.usable,
                )
            }
        }
    }

    fun updateBaseUrl(value: String) = _state.update { it.copy(baseUrl = value) }

    fun updateApiKey(value: String) = _state.update { it.copy(apiKey = value) }

    fun updateModel(value: String) = _state.update { it.copy(model = value) }

    /** 保存配置，同时会触发一轮补译。 */
    fun save() {
        val settings = _state.value.edited
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null, messageIsError = false) }
            repository.saveLlmSettings(settings)
            _state.update { it.copy(busy = false, configured = settings.usable) }
        }
    }

    /** 测试连接：用编辑中的值进行一次连接测试。 */
    fun test() {
        val settings = _state.value.edited
        if (!settings.usable) {
            _state.update { it.copy(message = Strings.LLM_TEST_EMPTY, messageIsError = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(testing = true, message = null, messageIsError = false) }
            val result = repository.testLlmConnection(settings)
            _state.update {
                it.copy(
                    testing = false,
                    message = result.fold(
                        onSuccess = { Strings.LLM_TEST_OK },
                        onFailure = { error ->
                            Strings.errorText((error as? AppError)?.kind ?: AppError.Kind.UNKNOWN)
                        },
                    ),
                    messageIsError = result.isFailure,
                )
            }
        }
    }

    /** 重试失败项：FAILED 复位为待译条目并立即进入翻译队列。 */
    fun retryFailed() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            repository.retryFailedTranslations()
            _state.update { it.copy(busy = false) }
        }
    }
}
