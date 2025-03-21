package xyz.xfqlittlefan.notdeveloper

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import java.io.File

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
    
    // Salva la configurazione del modulo e rende il file accessibile
    fun saveModuleConfig(context: Context, versionName: String, versionCode: String) {
        val prefs = getAppPreferences(context)
        prefs.edit().apply {
            putString("version_name", versionName)
            putString("version_code", versionCode)
            apply()
        }
    }
    
    // Ottieni la configurazione del modulo
    fun getModuleConfig(context: Context): ModuleConfig {
        val prefs = getAppPreferences(context)
        return ModuleConfig(
            versionName = prefs.getString("version_name", "") ?: "",
            versionCode = prefs.getString("version_code", "") ?: ""
        )
    }
    
    // Classe dati per la configurazione
    data class ModuleConfig(
        val versionName: String,
        val versionCode: String
    )
}
