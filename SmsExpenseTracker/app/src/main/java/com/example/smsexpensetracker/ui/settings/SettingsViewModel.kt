package com.example.smsexpensetracker.ui.settings

import androidx.lifecycle.*
import com.example.smsexpensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val initialBalanceText: String = "",
    val creditCardResetDay: Int    = 1,
    val isLoading: Boolean         = true,
    val isSaved: Boolean           = false,
    val balanceError: String?      = null,
    val resetDayError: String?     = null
)

class SettingsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            val balancePaise = repository.getInitialBalancePaise()
            val resetDay     = repository.getCreditCardResetDay()
            _state.update {
                it.copy(
                    initialBalanceText = if (balancePaise == 0L) "" else "%.2f".format(balancePaise / 100.0),
                    creditCardResetDay  = resetDay,
                    isLoading           = false
                )
            }
        }
    }

    fun onBalanceChange(v: String) = _state.update { it.copy(initialBalanceText = v, balanceError = null, isSaved = false) }

    fun onResetDayChange(day: Int) = _state.update { it.copy(creditCardResetDay = day, resetDayError = null, isSaved = false) }

    fun save() {
        val s = _state.value
        var hasError = false

        val rupees = s.initialBalanceText.trim().toDoubleOrNull()
        if (s.initialBalanceText.isNotBlank() && (rupees == null || rupees < 0)) {
            _state.update { it.copy(balanceError = "Enter a valid positive amount") }
            hasError = true
        }

        val day = s.creditCardResetDay
        if (day < 1 || day > 28) {
            _state.update { it.copy(resetDayError = "Day must be between 1 and 28") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val paise = if (rupees != null && rupees > 0) (rupees * 100).toLong() else 0L
            repository.setInitialBalancePaise(paise)
            repository.setCreditCardResetDay(day)
            repository.markSetupDone()
            _state.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}

class SettingsViewModelFactory(private val repo: ExpenseRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repo) as T
}
