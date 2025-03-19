package xyz.xfqlittlefan.notdeveloper

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences

object PreferencesHelper {
    
    private const val PREFERENCES_FILE = "xyz.xfqlittlefan.notdeveloper_preferences"
    
    // Per ottenere le preferenze dal contesto Xposed
    fun getXSharedPreferences(): XSharedPreferences {
        val prefs = XSharedPreferences("xyz.xfqlittlefan.notdeveloper", PREFERENCES_FILE)
        prefs.makeWorldReadable()
        return prefs
    }
    
    // Per ottenere le preferenze dall'app
    fun getAppPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_WORLD_READABLE)
    }
    
    // Salva la configurazione del modulo
    fun saveModuleConfig(context: Context, enabled: Boolean, versionName: String, versionCode: String) {
        val prefs = getAppPreferences(context)
        prefs.edit().apply {
            putBoolean("module_enabled", enabled)
            putString("version_name", versionName)
            putString("version_code", versionCode)
            apply()
        }
    }
    
    // Ottieni la configurazione del modulo
    fun getModuleConfig(context: Context): ModuleConfig {
        val prefs = getAppPreferences(context)
        return ModuleConfig(
            enabled = prefs.getBoolean("module_enabled", true),
            versionName = prefs.getString("version_name", "") ?: "",
            versionCode = prefs.getString("version_code", "") ?: ""
        )
    }
    
    // Classe dati per la configurazione
    data class ModuleConfig(
        val enabled: Boolean,
        val versionName: String,
        val versionCode: String
    )
}
