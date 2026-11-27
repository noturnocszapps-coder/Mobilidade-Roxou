package br.com.roxou.mobilidade

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.feature.finance.FinanceScreen
import br.com.roxou.mobilidade.feature.goals.GoalsScreen
import br.com.roxou.mobilidade.feature.rental.RentalScreen
import br.com.roxou.mobilidade.feature.trips.TripsScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    PANEL("painel", "Painel", Icons.Filled.Home),
    COMPENSA("compensa", "Compensa", Icons.Filled.CheckCircle),
    TRIPS("corridas", "Corridas", Icons.Filled.List),
    GOALS("metas", "Metas", Icons.Filled.Star),
    RENTAL("locacao", "Locação", Icons.Filled.DateRange),
    FINANCE("financeiro", "Finanças", Icons.Filled.ShoppingCart),
    SETTINGS("config", "Config", Icons.Filled.Settings),
}

@Composable
fun RoxouNavHost(repo: RoxouRepository) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.PANEL.route, modifier = Modifier.padding(padding)) {
            composable(Tab.PANEL.route) { DashboardScreen(repo) }
            composable(Tab.COMPENSA.route) { CompensaRoute(repo) }
            composable(Tab.TRIPS.route) { TripsScreen(repo) }
            composable(Tab.GOALS.route) { GoalsScreen(repo) }
            composable(Tab.RENTAL.route) { RentalScreen(repo) }
            composable(Tab.FINANCE.route) { FinanceScreen(repo) }
            composable(Tab.SETTINGS.route) { SettingsScreen(repo) }
        }
    }
}
