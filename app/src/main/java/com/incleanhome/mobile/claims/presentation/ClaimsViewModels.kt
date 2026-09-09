package com.incleanhome.mobile.claims.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.incleanhome.mobile.claims.data.ClaimFailure
import com.incleanhome.mobile.claims.data.ClaimResult
import com.incleanhome.mobile.claims.data.ClaimTrackResponse
import com.incleanhome.mobile.claims.data.ClaimType
import com.incleanhome.mobile.claims.data.ClaimsDataSource
import com.incleanhome.mobile.claims.data.ClaimsRepository
import com.incleanhome.mobile.claims.data.CreateClaimRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ClaimFormField { NAME, EMAIL, DESCRIPTION }

data class ClaimFormState(
    val type: String = ClaimType.CLAIM,
    val consumerName: String = "",
    val consumerDocument: String = "",
    val consumerEmail: String = "",
    val consumerPhone: String = "",
    val relatedService: String = "",
    val description: String = "",
    val consumerRequest: String = "",
    val invalidFields: Set<ClaimFormField> = emptySet(),
    val isSubmitting: Boolean = false,
    val failure: ClaimFailure? = null,
    val createdCode: String? = null
)

internal fun validateClaimForm(state: ClaimFormState): Set<ClaimFormField> = buildSet {
    if (state.consumerName.isBlank()) add(ClaimFormField.NAME)
    if (state.consumerEmail.isBlank() || !EMAIL_REGEX.matches(state.consumerEmail.trim())) {
        add(ClaimFormField.EMAIL)
    }
    if (state.description.isBlank()) add(ClaimFormField.DESCRIPTION)
}

class CreateClaimViewModel(
    private val repository: ClaimsDataSource = ClaimsRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClaimFormState())
    val uiState: StateFlow<ClaimFormState> = _uiState.asStateFlow()

    fun onTypeChange(value: String) {
        if (value in ClaimType.VALUES) _uiState.update { it.copy(type = value, failure = null) }
    }

    fun onNameChange(value: String) = updateField(ClaimFormField.NAME) { copy(consumerName = value) }
    fun onDocumentChange(value: String) = update { copy(consumerDocument = value) }
    fun onEmailChange(value: String) = updateField(ClaimFormField.EMAIL) { copy(consumerEmail = value) }
    fun onPhoneChange(value: String) = update { copy(consumerPhone = value) }
    fun onRelatedServiceChange(value: String) = update { copy(relatedService = value) }
    fun onDescriptionChange(value: String) = updateField(ClaimFormField.DESCRIPTION) { copy(description = value) }
    fun onConsumerRequestChange(value: String) = update { copy(consumerRequest = value) }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val invalidFields = validateClaimForm(state)
        if (invalidFields.isNotEmpty()) {
            _uiState.update { it.copy(invalidFields = invalidFields, failure = null) }
            return
        }

        val request = state.toRequest()
        _uiState.update { it.copy(isSubmitting = true, failure = null) }
        viewModelScope.launch {
            when (val result = repository.create(request)) {
                is ClaimResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, createdCode = result.data.code)
                }
                is ClaimResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, failure = result.failure)
                }
            }
        }
    }

    private fun update(transform: ClaimFormState.() -> ClaimFormState) {
        _uiState.update { it.transform().copy(failure = null) }
    }

    private fun updateField(
        field: ClaimFormField,
        transform: ClaimFormState.() -> ClaimFormState
    ) {
        _uiState.update {
            it.transform().copy(invalidFields = it.invalidFields - field, failure = null)
        }
    }

    private fun ClaimFormState.toRequest() = CreateClaimRequest(
        type = type,
        consumerName = consumerName.trim(),
        consumerDocument = consumerDocument.trim(),
        consumerEmail = consumerEmail.trim(),
        consumerPhone = consumerPhone.trim(),
        relatedService = relatedService.trim().takeIf(String::isNotEmpty),
        description = description.trim(),
        consumerRequest = consumerRequest.trim().takeIf(String::isNotEmpty)
    )

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CreateClaimViewModel() as T
        }
    }
}

data class ClaimTrackState(
    val code: String = "",
    val isLoading: Boolean = false,
    val codeRequired: Boolean = false,
    val failure: ClaimFailure? = null,
    val claim: ClaimTrackResponse? = null
)

class ClaimTrackViewModel(
    initialCode: String = "",
    private val repository: ClaimsDataSource = ClaimsRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClaimTrackState(code = initialCode))
    val uiState: StateFlow<ClaimTrackState> = _uiState.asStateFlow()

    init {
        if (initialCode.isNotBlank()) track()
    }

    fun onCodeChange(value: String) {
        _uiState.update { it.copy(code = value, codeRequired = false, failure = null, claim = null) }
    }

    fun track() {
        val state = _uiState.value
        if (state.isLoading) return
        val code = state.code.trim()
        if (code.isEmpty()) {
            _uiState.update { it.copy(codeRequired = true, failure = null, claim = null) }
            return
        }

        _uiState.update { it.copy(code = code, isLoading = true, failure = null, claim = null) }
        viewModelScope.launch {
            when (val result = repository.track(code)) {
                is ClaimResult.Success -> _uiState.update {
                    it.copy(isLoading = false, claim = result.data)
                }
                is ClaimResult.Error -> _uiState.update {
                    it.copy(isLoading = false, failure = result.failure)
                }
            }
        }
    }

    companion object {
        fun Factory(initialCode: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ClaimTrackViewModel(initialCode) as T
            }
    }
}

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
