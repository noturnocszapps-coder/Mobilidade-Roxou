package br.com.roxou.mobilidade.leitura.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.roxou.mobilidade.leitura.data.PreferencesManager

@Composable
fun PackagesScreen(prefsManager: PreferencesManager) {
    val monitoredPackages by prefsManager.monitoredPackages.collectAsState()
    val recentPackages by prefsManager.recentPackages.collectAsState()

    var newPackageInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pacotes Monitorados", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar")
            }
        }

        // Monitored list
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ativos para escuta:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (monitoredPackages.isEmpty()) {
                    Text("Nenhum pacote monitorado.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        monitoredPackages.forEach { pkg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pkg, style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = {
                                    val updated = monitoredPackages.toMutableSet()
                                    updated.remove(pkg)
                                    prefsManager.setMonitoredPackages(updated)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        Text("Vistos Recentemente", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (recentPackages.isEmpty()) {
                    Text("Nenhum pacote visto recentemente.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(recentPackages.toList()) { pkg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pkg, style = MaterialTheme.typography.bodySmall)
                                if (!monitoredPackages.contains(pkg)) {
                                    TextButton(onClick = {
                                        val updated = monitoredPackages.toMutableSet()
                                        updated.add(pkg)
                                        prefsManager.setMonitoredPackages(updated)
                                    }) {
                                        Text("Monitorar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar Pacote") },
            text = {
                OutlinedTextField(
                    value = newPackageInput,
                    onValueChange = { newPackageInput = it },
                    label = { Text("Nome do pacote (ex: com.ubercab.driver)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPackageInput.isNotBlank()) {
                        val updated = monitoredPackages.toMutableSet()
                        updated.add(newPackageInput.trim())
                        prefsManager.setMonitoredPackages(updated)
                        newPackageInput = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
