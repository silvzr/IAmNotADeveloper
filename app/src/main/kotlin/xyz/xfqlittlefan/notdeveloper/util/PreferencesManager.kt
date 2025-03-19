package xyz.xfqlittlefan.notdeveloper.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "time_modifier_prefs",
        Context.MODE_WORLD_READABLE
    )
    
    companion object {
        private const val KEY_CUSTOM_TIME = "custom_time_millis"
    }
    
    fun setCustomTimeMillis(timeMillis: Long) {
        prefs.edit().putLong(KEY_CUSTOM_TIME, timeMillis).apply()
    }
    
    fun getCustomTimeMillis(): Long {
        return prefs.getLong(KEY_CUSTOM_TIME, 0)
    }
}
