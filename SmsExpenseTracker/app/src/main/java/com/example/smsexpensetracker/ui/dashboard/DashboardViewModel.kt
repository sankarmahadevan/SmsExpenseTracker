package com.example.smsexpensetracker.ui.dashboard

import androidx.lifecycle.*
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

data class DashboardUiState(
    val savingsBalancePaise: Long       = 0L,
    val ccSpentPaise: Long              = 0L,
    val selectedMonth: YearMonth        = YearMonth.now(),
    val transactions: List<Transaction> = emptyList(),
    val monthlyDebitPaise: Long         = 0L,
    val monthlyCreditPaise: Long        = 0L,
    val isLoading: Boolean              = true
)

class DashboardViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _monthTransactions = _selectedMonth.flatMapLatest { month ->
        repository.getTransactionsForMonth(month)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedMonth,
        _monthTransactions
    ) { month, txns -> month to txns }
        .transformLatest { (month, txns) ->
            val summary = repository.getMonthSummary(month)
            emit(
                DashboardUiState(
                    savingsBalancePaise = repository.getSavingsBalance(),
                    ccSpentPaise        = repository.getCreditCardNetSpent(),
                    selectedMonth       = month,
                    transactions        = txns,
                    monthlyDebitPaise   = summary.totalDebitPaise,
                    monthlyCreditPaise  = summary.totalCreditPaise,
                    isLoading           = false
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun previousMonth() = _selectedMonth.update { it.minusMonths(1) }

    fun nextMonth() {
        val next = _selectedMonth.value.plusMonths(1)
        if (!next.isAfter(YearMonth.now())) _selectedMonth.update { next }
    }

    fun deleteTransaction(t: Transaction) = viewModelScope.launch { repository.deleteTransaction(t) }
}

class DashboardViewModelFactory(private val repo: ExpenseRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repo) as T
}
