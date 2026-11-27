package br.com.roxou.mobilidade.leitura.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajuda e Instruções", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aviso importante para Android 13+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Como este aplicativo foi instalado fora da Google Play Store (sideload), o Android 13 e versões mais recentes bloqueiam por padrão a ativação do Serviço de Acessibilidade (" +
                    "\"Configuração restrita\").",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Como liberar:", style = MaterialTheme.typography.titleSmall)
                Text("1. Abra as Configurações do seu celular.", style = MaterialTheme.typography.bodyMedium)
                Text("2. Vá em Aplicativos e procure por \"Roxou Mobilidade – Teste de Leitura\".", style = MaterialTheme.typography.bodyMedium)
                Text("3. Toque no menu de três pontos (⋮) no canto superior direito.", style = MaterialTheme.typography.bodyMedium)
                Text("4. Selecione \"Permitir configurações restritas\".", style = MaterialTheme.typography.bodyMedium)
                Text("5. Volte para a aba Status e toque em \"Abrir Configurações de Acessibilidade\" para ativar o serviço.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sobre o Aplicativo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Este aplicativo registra exclusivamente de forma local o que o serviço de acessibilidade enxerga nas telas da Uber Driver e da 99 Motorista para fins de diagnóstico. " +
                    "Não há envio de dados para servidores, sem chaves de API, sem Gemini e sem acesso à Internet.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
