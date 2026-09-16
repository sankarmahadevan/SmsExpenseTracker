package com.example.smsexpensetracker.ui.transactions

import androidx.lifecycle.*
import com.example.smsexpensetracker.data.model.*
import com.example.smsexpensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditUiState(
    val id: Long                     = -1L,
    val amountText: String           = "",
    val type: TransactionType        = TransactionType.DEBIT,
    val category: TransactionCategory = TransactionCategory.UPI,
    val accountType: AccountType     = AccountType.SAVINGS,
    val bankName: String             = "",
    val description: String          = "",
    val dateMillis: Long             = System.currentTimeMillis(),
    val isLoading: Boolean           = false,
    val isSaved: Boolean             = false,
    val error: String?               = null
)

class TransactionViewModel(
    private val repository: ExpenseRepository,
    private val editId: Long          // -1 = new, else = edit existing
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditUiState())
    val state: StateFlow<AddEditUiState> = _state.asStateFlow()

    init {
        if (editId > 0) loadExisting(editId)
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val t = repository.getTransactionById(id)
            if (t != null) {
                _state.update {
                    it.copy(
                        id          = t.id,
                        amountText  = "%.2f".format(t.amountPaise / 100.0),
                        type        = t.type,
                        category    = t.category,
                        accountType = t.accountType,
                        bankName    = t.bankName,
                        description = t.description,
                        dateMillis  = t.timestamp,
                        isLoading   = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "Transaction not found") }
            }
        }
    }

    fun onAmountChange(v: String)      = _state.update { it.copy(amountText = v, error = null) }
    fun onTypeChange(v: TransactionType) = _state.update { it.copy(type = v) }
    fun onCategoryChange(v: TransactionCategory) {
        val accountType = when (v) {
            TransactionCategory.CREDIT_CARD -> AccountType.CREDIT_CARD
            else                            -> AccountType.SAVINGS
        }
        _state.update { it.copy(category = v, accountType = accountType) }
    }
    fun onBankNameChange(v: String)    = _state.update { it.copy(bankName = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onDateChange(millis: Long)     = _state.update { it.copy(dateMillis = millis) }

    fun save() {
        val s = _state.value
        val rupeesText = s.amountText.trim()

        if (rupeesText.isBlank()) {
            _state.update { it.copy(error = "Amount is required") }
            return
        }
        val rupees = rupeesText.toDoubleOrNull()
        if (rupees == null || rupees <= 0) {
            _state.update { it.copy(error = "Enter a valid positive amount") }
            return
        }

        val paise = (rupees * 100).toLong()

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val transaction = Transaction(
                id          = if (s.id > 0) s.id else 0L,
                amountPaise = paise,
                type        = s.type,
                category    = s.category,
                accountType = s.accountType,
                bankName    = s.bankName.trim(),
                description = s.description.trim(),
                timestamp   = s.dateMillis,
                isManual    = true
            )
            if (s.id > 0) repository.updateTransaction(transaction)
            else          repository.insertTransaction(transaction)
            _state.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}

class TransactionViewModelFactory(
    private val repo: ExpenseRepository,
    private val editId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TransactionViewModel(repo, editId) as T
}
