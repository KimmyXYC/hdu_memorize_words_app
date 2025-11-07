package cn.nepuko.sklhelper.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserPreferences {
    private const val PREFS_NAME = "skl_helper_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_TARGET_SCORE = "target_score"
    private const val KEY_TARGET_TIME = "target_time"
    private const val KEY_AI_ASSIST_ENABLED = "ai_assist_enabled"
    private const val KEY_AUTO_UPDATE_QUESTION_BANK = "auto_update_question_bank"

    private const val DEFAULT_TARGET_SCORE = 100
    private const val DEFAULT_TARGET_TIME = 300
    private const val DEFAULT_AI_ASSIST_ENABLED = false
    private const val DEFAULT_AUTO_UPDATE_QUESTION_BANK = true

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _username.value = prefs.getString(KEY_USERNAME, null)
    }

    fun saveCredentials(context: Context, username: String, password: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            apply()
        }
        _username.value = username
    }

    fun clearCredentials(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _username.value = null
    }

    fun getUsername(context: Context): String? {
        if (_username.value == null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _username.value = prefs.getString(KEY_USERNAME, null)
        }
        return _username.value
    }

    fun getPassword(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PASSWORD, null)
    }

    fun saveTargetScore(context: Context, score: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_TARGET_SCORE, score).apply()
    }

    fun getTargetScore(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TARGET_SCORE, DEFAULT_TARGET_SCORE)
    }

    fun saveTargetTime(context: Context, time: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_TARGET_TIME, time).apply()
    }

    fun getTargetTime(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TARGET_TIME, DEFAULT_TARGET_TIME)
    }

    fun saveAiAssistEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AI_ASSIST_ENABLED, enabled).apply()
    }

    fun getAiAssistEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AI_ASSIST_ENABLED, DEFAULT_AI_ASSIST_ENABLED)
    }

    fun saveAutoUpdateQuestionBank(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_UPDATE_QUESTION_BANK, enabled).apply()
    }

    fun getAutoUpdateQuestionBank(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_UPDATE_QUESTION_BANK, DEFAULT_AUTO_UPDATE_QUESTION_BANK)
    }
}
