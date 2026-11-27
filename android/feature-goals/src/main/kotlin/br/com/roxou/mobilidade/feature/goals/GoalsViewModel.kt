package br.com.roxou.mobilidade.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class GoalsViewModel(private val repo: RoxouRepository) : ViewModel() {
    val state: StateFlow<AppState?> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Corrige o total da semana pelo número do painel da Uber (vazio remove a correção). */
    fun setUberPanelCount(weekId: LocalDate, text: String) = viewModelScope.launch {
        repo.setUberPanelCount(weekId, text.trim().toIntOrNull()?.takeIf { it >= 0 })
    }
}
