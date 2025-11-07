package cn.nepuko.sklhelper.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AiSettings(
    val apiUrl: String,
    val apiKey: String,
    val modelName: String,
    val temperature: String,
    val timeout: String,
    val retries: String
)

class AiSettingsRepository private constructor(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

    private object PreferencesKeys {
        val API_URL = stringPreferencesKey("api_url")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val TEMPERATURE = stringPreferencesKey("temperature")
        val TIMEOUT = stringPreferencesKey("timeout")
        val RETRIES = stringPreferencesKey("retries")
    }

    val aiSettingsFlow: Flow<AiSettings> = context.dataStore.data
        .map { preferences ->
            AiSettings(
                apiUrl = preferences[PreferencesKeys.API_URL] ?: "https://api.deepseek.com",
                apiKey = preferences[PreferencesKeys.API_KEY] ?: "",
                modelName = preferences[PreferencesKeys.MODEL_NAME] ?: "deepseek-chat",
                temperature = preferences[PreferencesKeys.TEMPERATURE] ?: "0.2",
                timeout = preferences[PreferencesKeys.TIMEOUT] ?: "15000",
                retries = preferences[PreferencesKeys.RETRIES] ?: "3"
            )
        }

    suspend fun saveAiSettings(settings: AiSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_URL] = settings.apiUrl
            preferences[PreferencesKeys.API_KEY] = settings.apiKey
            preferences[PreferencesKeys.MODEL_NAME] = settings.modelName
            preferences[PreferencesKeys.TEMPERATURE] = settings.temperature
            preferences[PreferencesKeys.TIMEOUT] = settings.timeout
            preferences[PreferencesKeys.RETRIES] = settings.retries
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AiSettingsRepository? = null

        fun getInstance(context: Context): AiSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AiSettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
