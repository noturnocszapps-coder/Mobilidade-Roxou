package br.com.roxou.mobilidade.leitura.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.roxou.mobilidade.leitura.data.AppDatabase
import br.com.roxou.mobilidade.leitura.data.SnapshotEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

@Composable
fun ExportScreen(context: Context) {
    val db = remember { AppDatabase.getDatabase(context) }
    val datesFlow = remember { db.snapshotDao().getExportableDates() }
    val dates by datesFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Exportar Registros", style = MaterialTheme.typography.headlineSmall)
        Text("Escolha uma data para gerar os arquivos .txt e .json do dia e compartilhar.", style = MaterialTheme.typography.bodyMedium)

        if (dates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nenhuma data disponível para exportação.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dates) { dateStr ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(dateStr, style = MaterialTheme.typography.titleMedium)
                            Button(
                                enabled = !isExporting,
                                onClick = {
                                    isExporting = true
                                    coroutineScope.launch {
                                        exportDateData(context, db, dateStr)
                                        isExporting = false
                                    }
                                }
                            ) {
                                Text(if (isExporting) "Gerando..." else "Exportar Dia")
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun exportDateData(context: Context, db: AppDatabase, dateStr: String) {
    withContext(Dispatchers.IO) {
        val snapshots = db.snapshotDao().getSnapshotsForExportSync(dateStr)
        if (snapshots.isEmpty()) return@withContext

        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val txtFile = File(cacheDir, "leitura_$dateStr.txt")
        val jsonFile = File(cacheDir, "leitura_$dateStr.json")

        // Write TXT gradually
        OutputStreamWriter(FileOutputStream(txtFile), "UTF-8").use { writer ->
            writer.write("=== ROXOU MOBILIDADE - RELATORIO DE LEITURA ($dateStr) ===\n\n")
            for (snap in snapshots) {
                writer.write("[${snap.timeString}] ${snap.packageName} (${snap.eventType})\n")
                writer.write("Nodes JSON: ${snap.nodeDumpJson}\n")
                writer.write("--------------------------------------------------\n")
            }
        }

        // Write JSON gradually
        val gson = Gson()
        OutputStreamWriter(FileOutputStream(jsonFile), "UTF-8").use { writer ->
            writer.write("[\n")
            snapshots.forEachIndexed { index, snap ->
                val jsonStr = gson.toJson(snap)
                writer.write(jsonStr)
                if (index < snapshots.size - 1) {
                    writer.write(",\n")
                } else {
                    writer.write("\n")
                }
            }
            writer.write("]\n")
        }

        // Share files via FileProvider
        val txtUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", txtFile)
        val jsonUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", jsonFile)

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(txtUri, jsonUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Compartilhar Registros de $dateStr").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
