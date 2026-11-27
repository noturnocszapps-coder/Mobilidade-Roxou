package br.com.roxou.mobilidade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF9B5DE5))) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TelaInicial(versao = BuildConfigInfo.versao(this))
                }
            }
        }
    }
}

@Composable
fun TelaInicial(versao: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Roxou Mobilidade",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "APK gerado pelo GitHub Actions.\nAs telas do app entram nas próximas etapas.",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "Versão $versao", style = MaterialTheme.typography.bodySmall)
    }
}

object BuildConfigInfo {
    fun versao(activity: ComponentActivity): String =
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "?"
}
