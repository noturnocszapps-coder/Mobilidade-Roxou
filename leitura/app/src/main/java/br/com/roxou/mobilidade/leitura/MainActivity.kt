package br.com.roxou.mobilidade.leitura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import br.com.roxou.mobilidade.leitura.data.PreferencesManager
import br.com.roxou.mobilidade.leitura.ui.components.MainScaffold
import br.com.roxou.mobilidade.leitura.ui.theme.Theme.RoxouMobilidadeLeituraTheme

class MainActivity : ComponentActivity() {
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsManager = PreferencesManager(this)

        setContent {
            RoxouMobilidadeLeituraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScaffold(prefsManager = prefsManager)
                }
            }
        }
    }
}
