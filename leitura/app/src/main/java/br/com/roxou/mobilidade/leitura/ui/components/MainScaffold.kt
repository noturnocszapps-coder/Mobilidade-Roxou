package br.com.roxou.mobilidade.leitura.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.default.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import br.com.roxou.mobilidade.leitura.data.PreferencesManager
import br.com.roxou.mobilidade.leitura.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Status : Screen("status", "Status", Icons.Default.Home)
    object Records : Screen("records", "Registros", Icons.Default.List)
    object Packages : Screen("packages", "Pacotes", Icons.Default.Settings)
    object Export : Screen("export", "Exportar", Icons.Default.Share)
    object Help : Screen("help", "Ajuda", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(prefsManager: PreferencesManager) {
    val context = LocalContext.current
    val items = listOf(
        Screen.Status,
        Screen.Records,
        Screen.Packages,
        Screen.Export,
        Screen.Help
    )
    var selectedRoute by remember { mutableStateOf<String>(Screen.Status.route) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roxou Leitura") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedRoute == screen.route,
                        onClick = { selectedRoute = screen.route }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedRoute) {
                Screen.Status.route -> StatusScreen(prefsManager)
                Screen.Records.route -> RecordsScreen(context)
                Screen.Packages.route -> PackagesScreen(prefsManager)
                Screen.Export.route -> ExportScreen(context)
                Screen.Help.route -> HelpScreen()
            }
        }
    }
}
