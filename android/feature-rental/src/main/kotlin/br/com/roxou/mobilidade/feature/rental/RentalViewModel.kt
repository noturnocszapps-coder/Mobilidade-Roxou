package br.com.roxou.mobilidade.feature.rental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.domain.km.KmRecord
import br.com.roxou.mobilidade.domain.rental.RentalPayment
import br.com.roxou.mobilidade.domain.rental.RentalPaymentSource
import br.com.roxou.mobilidade.domain.trips.ManualTripEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class RentalViewModel(private val repo: RoxouRepository) : ViewModel() {
    val state: StateFlow<AppState?> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addPayment(weekId: LocalDate, amount: String, source: RentalPaymentSource, confirmed: Boolean): String? {
        val cents = ManualTripEntry.parseMoneyToCents(amount)?.takeIf { it > 0 } ?: return "Valor inválido."
        viewModelScope.launch {
            repo.saveRentalPayment(RentalPayment(repo.newId(), weekId, cents, source, repo.now(), confirmed))
        }
        return null
    }

    fun setConfirmed(id: String, confirmed: Boolean) = viewModelScope.launch { repo.setRentalPaymentConfirmed(id, confirmed) }
    fun deletePayment(id: String) = viewModelScope.launch { repo.deleteRentalPayment(id) }

    fun addKm(work: String, privateText: String): String? {
        val w = work.trim().replace(',', '.').ifEmpty { "0" }.toDoubleOrNull()
        val p = privateText.trim().replace(',', '.').ifEmpty { "0" }.toDoubleOrNull()
        if (w == null || p == null || w < 0 || p < 0 || (w == 0.0 && p == 0.0)) return "Km inválido."
        viewModelScope.launch { repo.saveKm(KmRecord(repo.newId(), repo.now(), w, p)) }
        return null
    }

    fun deleteKm(id: String) = viewModelScope.launch { repo.deleteKm(id) }
}
