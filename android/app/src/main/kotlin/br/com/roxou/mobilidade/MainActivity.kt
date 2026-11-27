package br.com.roxou.mobilidade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.roxou.mobilidade.ui.RoxouTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as RoxouApp).repository
        setContent {
            RoxouTheme { RoxouNavHost(repo) }
        }
    }
}
