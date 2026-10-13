/**
 * 偏好设置（DataStore Preferences）：LLM 接口配置等用户级开关。
 */
package com.animechecker.local.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.animechecker.local.domain.model.LlmSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * 设置项读写
 */
internal class SettingsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val llmSettingsFlow: Flow<LlmSettings> = dataStore.data.map { prefs ->
        LlmSettings(
            baseUrl = prefs[KEY_LLM_BASE_URL].orEmpty(),
            apiKey = prefs[KEY_LLM_API_KEY].orEmpty(),
            model = prefs[KEY_LLM_MODEL].orEmpty(),
        )
    }

    suspend fun llmSettings(): LlmSettings = llmSettingsFlow.first()

    /** 保存前统一去空白，接口地址去掉尾部斜杠。 */
    suspend fun saveLlmSettings(settings: LlmSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_LLM_BASE_URL] = settings.baseUrl.trim().trimEnd('/')
            prefs[KEY_LLM_API_KEY] = settings.apiKey.trim()
            prefs[KEY_LLM_MODEL] = settings.model.trim()
        }
    }

    val lastRefreshAtFlow: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_REFRESH_AT] ?: 0L
    }

    suspend fun setLastRefreshAt(at: Long) {
        dataStore.edit { prefs -> prefs[KEY_LAST_REFRESH_AT] = at }
    }

    private companion object {
        val KEY_LLM_BASE_URL = stringPreferencesKey("llm_base_url")
        val KEY_LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val KEY_LLM_MODEL = stringPreferencesKey("llm_model")
        val KEY_LAST_REFRESH_AT = longPreferencesKey("last_refresh_at")
    }
}
