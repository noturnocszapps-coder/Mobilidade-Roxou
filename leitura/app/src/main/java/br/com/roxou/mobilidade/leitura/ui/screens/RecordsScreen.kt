package br.com.roxou.mobilidade.leitura.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.roxou.mobilidade.leitura.data.AppDatabase
import br.com.roxou.mobilidade.leitura.data.NodeInfoItem
import br.com.roxou.mobilidade.leitura.data.SnapshotEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun RecordsScreen(context: android.content.Context) {
    val db = remember { AppDatabase.getDatabase(context) }
    val snapshotsFlow = remember { db.snapshotDao().getAllSnapshots() }
    val snapshots by snapshotsFlow.collectAsState(initial = emptyList())

    var selectedSnapshot by remember { mutableStateOf<SnapshotEntity?>(null) }
    val gson = remember { Gson() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Registros Capturados", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        if (snapshots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nenhum registro encontrado.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snapshots) { snapshot ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSnapshot = snapshot }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = snapshot.packageName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${snapshot.dateString} ${snapshot.timeString}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tipo: ${snapshot.eventType}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedSnapshot != null) {
        val snapshot = selectedSnapshot!!
        val listType = object : TypeToken<List<NodeInfoItem>>() {}.type
        val nodes: List<NodeInfoItem> = try {
            gson.fromJson(snapshot.nodeDumpJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        AlertDialog(
            onDismissRequest = { selectedSnapshot = null },
            title = {
                Column {
                    Text("Detalhes do Retrato", style = MaterialTheme.typography.titleMedium)
                    Text("${snapshot.packageName} • ${snapshot.timeString}", style = MaterialTheme.typography.labelSmall)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(nodes) { node ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (!node.viewIdResourceName.isNullOrBlank()) {
                                    Text(
                                        text = "ID: ${node.viewIdResourceName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!node.text.isNullOrBlank()) {
                                    Text(
                                        text = "Texto: ${node.text}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (!node.contentDescription.isNullOrBlank()) {
                                    Text(
                                        text = "Desc: ${node.contentDescription}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSnapshot = null }) {
                    Text("Fechar")
                }
            }
        )
    }
}
