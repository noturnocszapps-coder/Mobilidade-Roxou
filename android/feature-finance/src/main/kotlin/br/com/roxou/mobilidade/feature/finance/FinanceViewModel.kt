package br.com.roxou.mobilidade.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.domain.fuel.FuelLog
import br.com.roxou.mobilidade.domain.model.EarningCategory
import br.com.roxou.mobilidade.domain.model.Expense
import br.com.roxou.mobilidade.domain.model.ExpenseCategory
import br.com.roxou.mobilidade.domain.model.ExtraEarning
import br.com.roxou.mobilidade.domain.model.PaymentMethod
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.trips.ManualTripEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(private val repo: RoxouRepository) : ViewModel() {
    val state: StateFlow<AppState?> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun money(text: String) = ManualTripEntry.parseMoneyToCents(text)?.takeIf { it > 0 }
    private fun decimal(text: String) = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

    fun addExtra(platform: Platform, category: EarningCategory, amount: String, payment: PaymentMethod): String? {
        val cents = money(amount) ?: return "Valor inválido."
        viewModelScope.launch { repo.saveExtra(ExtraEarning(repo.newId(), platform, category, repo.now(), cents, payment)) }
        return null
    }

    fun addExpense(category: ExpenseCategory, amount: String): String? {
        val cents = money(amount) ?: return "Valor inválido."
        viewModelScope.launch { repo.saveExpense(Expense(repo.newId(), category, repo.now(), cents)) }
        return null
    }

    fun addFuel(amount: String, liters: String, odometer: String, fullTank: Boolean): String? {
        val cents = money(amount) ?: return "Valor inválido."
        val l = decimal(liters) ?: return "Litros inválidos."
        val odo = decimal(odometer) ?: return "Odômetro inválido."
        viewModelScope.launch { repo.saveFuel(FuelLog(repo.newId(), null, repo.now(), odo, l, cents, fullTank)) }
        return null
    }

    fun deleteExtra(id: String) = viewModelScope.launch { repo.deleteExtra(id) }
    fun deleteExpense(id: String) = viewModelScope.launch { repo.deleteExpense(id) }
    fun deleteFuel(id: String) = viewModelScope.launch { repo.deleteFuel(id) }
}
