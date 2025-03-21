package xyz.xfqlittlefan.notdeveloper

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    
    private lateinit var versionNameEdit: EditText
    private lateinit var versionCodeEdit: EditText
    private lateinit var versionNameToggle: ToggleButton
    private lateinit var versionCodeToggle: ToggleButton
    private lateinit var spotifyPatchToggle: ToggleButton
    private lateinit var statusText: TextView
    
    // Pattern per validare il versionName: X.Y.ZZ.AAA
    private val versionNamePattern = Pattern.compile("^\\d+\\.\\d+\\.\\d{2}\\.\\d{3}$")
    
    // Pattern per validare il versionCode: deve avere esattamente 9 cifre
    private val versionCodePattern = Pattern.compile("^\\d{9}$")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inizializza le views
        versionNameEdit = findViewById(R.id.versionNameEdit)
        versionCodeEdit = findViewById(R.id.versionCodeEdit)
        versionNameToggle = findViewById(R.id.versionNameToggle)
        versionCodeToggle = findViewById(R.id.versionCodeToggle)
        spotifyPatchToggle = findViewById(R.id.spotifyPatchToggle)
        statusText = findViewById(R.id.statusText)
        
        // Carica la configurazione salvata
        loadSavedConfig()
        
        // Aggiungi TextWatcher per validare quando i valori cambiano
        versionNameEdit.addTextChangedListener(createTextWatcher { validateField(true) })
        versionCodeEdit.addTextChangedListener(createTextWatcher { validateField(false) })
        
        // Aggiungi listener per i toggle button
        versionNameToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                validateAndSaveVersionName()
            } else {
                // Disattiva lo spoofing per versionName
                PreferencesHelper.saveModuleConfig(
                    this, 
                    "", 
                    if (versionCodeToggle.isChecked) versionCodeEdit.text.toString() else "",
                    spotifyPatchToggle.isChecked
                )
                updateStatus()
            }
        }
        
        versionCodeToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                validateAndSaveVersionCode()
            } else {
                // Disattiva lo spoofing per versionCode
                PreferencesHelper.saveModuleConfig(
                    this, 
                    if (versionNameToggle.isChecked) versionNameEdit.text.toString() else "", 
                    "",
                    spotifyPatchToggle.isChecked
                )
                updateStatus()
            }
        }
        
        spotifyPatchToggle.setOnCheckedChangeListener { _, isChecked ->
            saveSpotifyPatchConfig(isChecked)
            updateStatus()
        }
    }
    
    private fun loadSavedConfig() {
        val config = PreferencesHelper.getModuleConfig(this)
        
        // Imposta i valori salvati
        if (config.versionName.isNotEmpty()) {
            versionNameEdit.setText(config.versionName)
            versionNameToggle.isChecked = true
        } else {
            versionNameToggle.isChecked = false
        }
        
        if (config.versionCode.isNotEmpty()) {
            versionCodeEdit.setText(config.versionCode)
            versionCodeToggle.isChecked = true
        } else {
            versionCodeToggle.isChecked = false
        }
        
        spotifyPatchToggle.isChecked = config.spotifyPatchEnabled
        
        // Aggiorna lo stato di abilitazione dei toggle
        updateToggleStates()
        
        // Aggiorna lo stato iniziale
        updateStatus()
    }
    
    private fun validateField(isVersionName: Boolean) {
        if (isVersionName) {
            val versionName = versionNameEdit.text.toString().trim()
            val isValid = versionName.isEmpty() || versionNamePattern.matcher(versionName).matches()
            
            // Aggiorna lo stato di abilitazione del toggle
            updateVersionNameToggleState()
            
            if (versionNameToggle.isChecked && isValid) {
                validateAndSaveVersionName()
            }
        } else {
            val versionCode = versionCodeEdit.text.toString().trim()
            val isValid = versionCode.isEmpty() || versionCodePattern.matcher(versionCode).matches()
            
            // Aggiorna lo stato di abilitazione del toggle
            updateVersionCodeToggleState()
            
            if (versionCodeToggle.isChecked && isValid) {
                validateAndSaveVersionCode()
            }
        }
    }
    
    private fun validateAndSaveVersionName() {
        val versionName = versionNameEdit.text.toString().trim()
        
        if (versionName.isEmpty() || !versionNamePattern.matcher(versionName).matches()) {
            versionNameToggle.isChecked = false
            updateStatus()
            return
        }
        
        // Salva la configurazione
        PreferencesHelper.saveModuleConfig(
            this, 
            versionName, 
            if (versionCodeToggle.isChecked) versionCodeEdit.text.toString() else "",
            spotifyPatchToggle.isChecked
        )
        updateStatus()
    }
    
    private fun validateAndSaveVersionCode() {
        val versionCode = versionCodeEdit.text.toString().trim()
        
        if (versionCode.isEmpty() || !versionCodePattern.matcher(versionCode).matches()) {
            versionCodeToggle.isChecked = false
            updateStatus()
            return
        }
        
        // Salva la configurazione
        PreferencesHelper.saveModuleConfig(
            this, 
            if (versionNameToggle.isChecked) versionNameEdit.text.toString() else "", 
            versionCode,
            spotifyPatchToggle.isChecked
        )
        updateStatus()
    }
    
    private fun saveSpotifyPatchConfig(enabled: Boolean) {
        PreferencesHelper.saveModuleConfig(
            this,
            if (versionNameToggle.isChecked) versionNameEdit.text.toString() else "",
            if (versionCodeToggle.isChecked) versionCodeEdit.text.toString() else "",
            enabled
        )
    }
    
    private fun updateStatus() {
        val versionName = versionNameEdit.text.toString().trim()
        val versionCode = versionCodeEdit.text.toString().trim()
        
        val statusMessage = StringBuilder()
        
        // Stato per versionName
        if (versionNameToggle.isChecked) {
            if (versionNamePattern.matcher(versionName).matches()) {
                statusMessage.append("✓ VersionName verrà impostato a: $versionName\n\n")
            } else {
                statusMessage.append("❌ VersionName non valido. Formato richiesto: X.Y.ZZ.AAA\n\n")
                versionNameToggle.isChecked = false
            }
        } else {
            statusMessage.append("ℹ️ VersionName originale non verrà modificato\n\n")
        }
        
        // Stato per versionCode
        if (versionCodeToggle.isChecked) {
            if (versionCodePattern.matcher(versionCode).matches()) {
                statusMessage.append("✓ VersionCode verrà impostato a: $versionCode\n\n")
            } else {
                statusMessage.append("❌ VersionCode non valido. Deve avere esattamente 9 cifre\n\n")
                versionCodeToggle.isChecked = false
            }
        } else {
            statusMessage.append("ℹ️ VersionCode originale non verrà modificato\n\n")
        }
        
        // Stato per Spotify Premium
        if (spotifyPatchToggle.isChecked) {
            statusMessage.append("✓ Spotify Premium sbloccato\n\n")
        } else {
            statusMessage.append("ℹ️ Spotify Premium non modificato\n\n")
        }
        
        statusText.text = statusMessage.toString().trim()
        
        // Aggiorna lo stato di abilitazione dei toggle dopo aver aggiornato lo stato
        updateToggleStates()
    }
    
    /**
     * Aggiorna lo stato di abilitazione di entrambi i toggle button
     */
    private fun updateToggleStates() {
        updateVersionNameToggleState()
        updateVersionCodeToggleState()
    }
    
    /**
     * Aggiorna lo stato di abilitazione del toggle di versionName
     */
    private fun updateVersionNameToggleState() {
        val versionName = versionNameEdit.text.toString().trim()
        val isValid = !versionName.isEmpty() && versionNamePattern.matcher(versionName).matches()
        versionNameToggle.isEnabled = isValid
    }
    
    /**
     * Aggiorna lo stato di abilitazione del toggle di versionCode
     */
    private fun updateVersionCodeToggleState() {
        val versionCode = versionCodeEdit.text.toString().trim()
        val isValid = !versionCode.isEmpty() && versionCodePattern.matcher(versionCode).matches()
        versionCodeToggle.isEnabled = isValid
    }
    
    // Helper per creare TextWatcher
    private fun createTextWatcher(afterTextChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                afterTextChanged()
            }
        }
    }
}
