package xyz.xfqlittlefan.notdeveloper

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    
    private lateinit var versionNameEdit: EditText
    private lateinit var versionCodeEdit: EditText
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
        statusText = findViewById(R.id.statusText)
        
        // Carica la configurazione salvata
        loadSavedConfig()
        
        // Aggiungi TextWatcher per salvare automaticamente quando i valori cambiano
        versionNameEdit.addTextChangedListener(createTextWatcher { validateAndSave() })
        versionCodeEdit.addTextChangedListener(createTextWatcher { validateAndSave() })
    }
    
    private fun loadSavedConfig() {
        val config = PreferencesHelper.getModuleConfig(this)
        
        // Imposta i valori salvati
        if (config.versionName.isNotEmpty()) {
            versionNameEdit.setText(config.versionName)
        }
        
        if (config.versionCode.isNotEmpty()) {
            versionCodeEdit.setText(config.versionCode)
        }
        
        // Aggiorna lo stato iniziale
        validateAndSave(false)
    }
    
    private fun validateAndSave(saveValues: Boolean = true) {
        val versionName = versionNameEdit.text.toString().trim()
        val versionCode = versionCodeEdit.text.toString().trim()
        
        val statusMessage = StringBuilder()
        
        // Validazione e notifica per versionName
        val isVersionNameValid = versionName.isNotEmpty() && versionNamePattern.matcher(versionName).matches()
        if (versionName.isNotEmpty() && !isVersionNameValid) {
            statusMessage.append("Version Name non valido. Formato richiesto: X.Y.ZZ.AAA (es. 9.0.33.782)\n")
            statusMessage.append("Il version name originale dell'app non verrà modificato.\n\n")
        }
        
        // Validazione e notifica per versionCode
        val isVersionCodeValid = versionCode.isNotEmpty() && versionCodePattern.matcher(versionCode).matches()
        if (versionCode.isNotEmpty() && !isVersionCodeValid) {
            statusMessage.append("Version Code non valido. Deve avere esattamente 9 cifre.\n")
            statusMessage.append("Il version code originale dell'app non verrà modificato.\n\n")
        }
        
        // Controllo se almeno uno dei due valori è valido
        val isAnyValueValid = isVersionNameValid || isVersionCodeValid
        
        // Aggiorna il testo di stato
        if (isAnyValueValid) {
            // Aggiungi i dettagli sui valori che saranno applicati
            if (statusMessage.isNotEmpty()) {
                // Ci sono alcuni valori non validi, ma almeno uno valido
                statusMessage.append("Valori che saranno applicati:\n")
                if (isVersionNameValid) {
                    statusMessage.append("- Version Name: $versionName\n")
                }
                if (isVersionCodeValid) {
                    statusMessage.append("- Version Code: $versionCode\n")
                }
                statusText.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
            } else {
                // Tutti i valori specificati sono validi
                statusMessage.append("Configurazione valida e salvata.\n")
                if (isVersionNameValid) {
                    statusMessage.append("- Version Name: $versionName\n")
                }
                if (isVersionCodeValid) {
                    statusMessage.append("- Version Code: $versionCode\n")
                }
                statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            }
            
            // Salva i valori (anche quelli non validi - il modulo Xposed farà la validazione)
            if (saveValues) {
                // Correzione: passaggio corretto dei parametri
                PreferencesHelper.saveModuleConfig(this, versionName, versionCode)
            }
        } else if (versionName.isEmpty() && versionCode.isEmpty()) {
            // Nessun valore specificato
            statusMessage.append("Specifica almeno un valore tra Version Name e Version Code")
            statusText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        } else {
            // Entrambi i valori specificati ma non validi
            statusMessage.append("Nessun valore valido specificato. Le app manterranno i loro valori originali.")
            statusText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            
            // Salva comunque i valori (il modulo Xposed non li applicherà)
            if (saveValues) {
                // Correzione: passaggio corretto dei parametri
                PreferencesHelper.saveModuleConfig(this, versionName, versionCode)
            }
        }
        
        statusText.text = statusMessage.toString().trim()
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
