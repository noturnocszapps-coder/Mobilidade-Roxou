package br.com.roxou.mobilidade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import br.com.roxou.mobilidade.data.conta.LicencaStore
import br.com.roxou.mobilidade.data.conta.RoxouApi
import kotlinx.coroutines.launch
import br.com.roxou.mobilidade.ui.RoxouTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as RoxouApp).repository
        // Renova a licença em silêncio quando houver internet. Sem internet, segue com a guardada.
        val licencas = LicencaStore.get(this)
        lifecycleScope.launch { RoxouApi(licencas).renovarSePreciso() }
        if (licencas.testador) lifecycleScope.launch { licencas.sincronizarHora() }
        setContent {
            RoxouTheme { RoxouNavHost(repo) }
        }
    }
}
