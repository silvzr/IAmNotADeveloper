package xyz.xfqlittlefan.notdeveloper.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import xyz.xfqlittlefan.notdeveloper.R
import xyz.xfqlittlefan.notdeveloper.util.PreferencesManager
import xyz.xfqlittlefan.notdeveloper.ui.theme.IAmNotADeveloperTheme
import xyz.xfqlittlefan.notdeveloper.xposed.isModuleActive
import xyz.xfqlittlefan.notdeveloper.xposed.isPreferencesReady
import java.text.SimpleDateFormat
import java.util.*

class TimeSettingsActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var datePickerButton: Button
    private lateinit var timePickerButton: Button
    private lateinit var selectedDateTimeTextView: TextView
    
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_settings)

        preferencesManager = PreferencesManager(this)
        
        // Inizializza le viste
        datePickerButton = findViewById(R.id.datePickerButton)
        timePickerButton = findViewById(R.id.timePickerButton)
        selectedDateTimeTextView = findViewById(R.id.selectedDateTimeTextView)

        // Carica data e ora salvate
        val savedTimeMillis = preferencesManager.getCustomTimeMillis()
        if (savedTimeMillis != 0L) {
            calendar.timeInMillis = savedTimeMillis
            updateDateTimeText()
        }

        // Configurazione date picker
        datePickerButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateTimeText()
                    saveDateTime()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Configurazione time picker
        timePickerButton.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    updateDateTimeText()
                    saveDateTime()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun updateDateTimeText() {
        selectedDateTimeTextView.text = dateFormat.format(calendar.time)
    }

    private fun saveDateTime() {
        preferencesManager.setCustomTimeMillis(calendar.timeInMillis)
    }
}
