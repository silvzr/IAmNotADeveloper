package xyz.xfqlittlefan.notdeveloper

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var moduleSwitch: Switch
    private lateinit var versionNameEdit: EditText
    private lateinit var versionCodeEdit: EditText
    private lateinit var saveButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inizializza le views
        moduleSwitch = findViewById(R.id.moduleSwitch)
        versionNameEdit = findViewById(R.id.versionNameEdit)
        versionCodeEdit = findViewById(R.id.versionCodeEdit)
        saveButton = findViewById(R.id.saveButton)
        
        // Carica la configurazione salvata
        loadSavedConfig()
        
        // Imposta il listener per il pulsante di salvataggio
        saveButton.setOnClickListener {
            saveConfig()
        }
    }
    
    private fun loadSavedConfig() {
        val config = PreferencesHelper.getModuleConfig(this)
        moduleSwitch.isChecked = config.enabled
        
        if (config.versionName.isNotEmpty()) {
            versionNameEdit.setText(config.versionName)
        }
        
        if (config.versionCode.isNotEmpty()) {
            versionCodeEdit.setText(config.versionCode)
        }
    }
    
    private fun saveConfig() {
        val enabled = moduleSwitch.isChecked
        val versionName = versionNameEdit.text.toString().trim()
        val versionCode = versionCodeEdit.text.toString().trim()
        
        // Validazione semplice
        if (versionName.isEmpty() && versionCode.isEmpty()) {
            Toast.makeText(this, "Specifica almeno un valore tra version name e version code", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validazione del version code
        if (versionCode.isNotEmpty()) {
            try {
                versionCode.toLong()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Il version code deve essere un numero valido", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // Salva la configurazione
        PreferencesHelper.saveModuleConfig(this, enabled, versionName, versionCode)
        Toast.makeText(this, "Configurazione salvata", Toast.LENGTH_SHORT).show()
    }
}
