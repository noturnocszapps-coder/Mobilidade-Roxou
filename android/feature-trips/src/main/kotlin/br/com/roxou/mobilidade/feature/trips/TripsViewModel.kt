package br.com.roxou.mobilidade.feature.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.domain.trips.ManualTripEntry
import br.com.roxou.mobilidade.domain.trips.ManualTripForm
import br.com.roxou.mobilidade.domain.trips.ManualTripResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripsViewModel(private val repo: RoxouRepository) : ViewModel() {
    val state: StateFlow<AppState?> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun now() = repo.now()

    /** Grava a corrida lançada à mão. Devolve a mensagem de erro, ou null se gravou. */
    fun addManual(form: ManualTripForm, onResult: (String?) -> Unit) {
        when (val r = ManualTripEntry.build(form, repo.newId(), repo.now())) {
            is ManualTripResult.Error -> onResult(r.message)
            is ManualTripResult.Ok -> viewModelScope.launch {
                repo.saveTrip(r.trip)
                onResult(null)
            }
        }
    }

    fun setConfirmed(id: String, confirmed: Boolean) = viewModelScope.launch { repo.setTripConfirmed(id, confirmed) }
    fun delete(id: String) = viewModelScope.launch { repo.deleteTrip(id) }
}
