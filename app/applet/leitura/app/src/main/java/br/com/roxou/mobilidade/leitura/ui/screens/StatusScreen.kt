package br.com.roxou.mobilidade.leitura.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.roxou.mobilidade.leitura.data.AppDatabase
import br.com.roxou.mobilidade.leitura.data.PreferencesManager
import br.com.roxou.mobilidade.leitura.service.LeituraAccessibilityService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatusScreen(prefsManager: PreferencesManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isRecording by prefsManager.isRecording.collectAsState()
    val maskPhones by prefsManager.maskPhones.collectAsState()

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var todayCount by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val db = remember { AppDatabase.getDatabase(context) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    fun checkAccessibility() {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val serviceString = "${context.packageName}/${LeituraAccessibilityService::class.java.name}"
        isAccessibilityEnabled = enabledServices.contains(serviceString) || am.isEnabled
    }

    LaunchedEffect(Unit) {
        checkAccessibility()
        db.snapshotDao().getCountForDate(todayStr).collect { count ->
            todayCount = count
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Status do Diagnóstico", style = MaterialTheme.typography.headlineSmall)

        // Accessibility status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isAccessibilityEnabled) "Acessibilidade: ATIVADA" else "Acessibilidade: DESLIGADA",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) {
                    Text("Abrir Configurações de Acessibilidade")
                }
            }
        }

        // Recording switch
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gravar agora", style = MaterialTheme.typography.titleMedium)
                    Text("Capturar eventos dos apps monitorados", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = isRecording,
                    onCheckedChange = { prefsManager.setRecording(it) }
                )
            }
        }

        // Mask phone numbers switch
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mascarar números de telefone", style = MaterialTheme.typography.titleMedium)
                    Text("Substitui sequências numéricas por ***", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = maskPhones,
                    onCheckedChange = { prefsManager.setMaskPhones(it) }
                )
            }
        }

        // Counter card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Retratos capturados hoje ($todayStr):", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$todayCount", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.typography.headlineMedium.color)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Delete all button
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Apagar todos os registros")
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar exclusão") },
            text = { Text("Deseja realmente apagar todos os registros salvos no banco de dados?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    coroutineScope.launch {
                        db.snapshotDao().deleteAll()
                    }
                }) {
                    Text("Apagar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
