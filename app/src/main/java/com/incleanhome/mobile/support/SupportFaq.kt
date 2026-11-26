package com.incleanhome.mobile.support

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.incleanhome.mobile.R
import com.incleanhome.mobile.experiments.Assignment
import com.incleanhome.mobile.experiments.Experiment
import com.incleanhome.mobile.experiments.ExperimentService
import com.incleanhome.mobile.experiments.Experimentation
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingPlan
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Categories of US38 (Escenario 1). */
enum class FaqCategory(val id: String, @param:StringRes val label: Int) {
    BOOKINGS("bookings", R.string.faq_category_bookings),
    ACCOUNT("account_access", R.string.faq_category_account),
    WORKERS("workers", R.string.faq_category_workers),
    MEMBERSHIPS("memberships", R.string.faq_category_memberships)
}

data class FaqItem(
    val id: String,
    val category: FaqCategory,
    @param:StringRes val question: Int,
    @param:StringRes val answer: Int
)

object FaqCatalog {
    val items: List<FaqItem> = listOf(
        FaqItem("bookings-amount", FaqCategory.BOOKINGS, R.string.faq_bookings_amount_q, R.string.faq_bookings_amount_a),
        FaqItem("bookings-cancel", FaqCategory.BOOKINGS, R.string.faq_bookings_cancel_q, R.string.faq_bookings_cancel_a),
        FaqItem("bookings-review", FaqCategory.BOOKINGS, R.string.faq_bookings_review_q, R.string.faq_bookings_review_a),
        FaqItem("account-code", FaqCategory.ACCOUNT, R.string.faq_account_code_q, R.string.faq_account_code_a),
        FaqItem("account-attempts", FaqCategory.ACCOUNT, R.string.faq_account_attempts_q, R.string.faq_account_attempts_a),
        FaqItem("workers-contact", FaqCategory.WORKERS, R.string.faq_workers_contact_q, R.string.faq_workers_contact_a),
        FaqItem("workers-verified", FaqCategory.WORKERS, R.string.faq_workers_verified_q, R.string.faq_workers_verified_a),
        FaqItem("memberships-pending", FaqCategory.MEMBERSHIPS, R.string.faq_memberships_pending_q, R.string.faq_memberships_pending_a)
    )

    fun byCategory(category: FaqCategory): List<FaqItem> = items.filter { it.category == category }
}

data class SupportUiState(
    val category: FaqCategory = FaqCategory.BOOKINGS,
    val expandedFaqId: String? = null,
    val resolvedFaqIds: Set<String> = emptySet()
)

class SupportViewModel(
    experiments: ExperimentService? = Experimentation.experimentsOrNull(),
    private val tracker: EventTracker? = Experimentation.trackerOrNull()
) : ViewModel() {
    private val visitId = UUID.randomUUID().toString()
    private val assignment: Assignment =
        experiments?.assignment(Experiment.SUPPORT_FAQ) ?: Assignment.inactive(Experiment.SUPPORT_FAQ)
    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    fun selectCategory(category: FaqCategory) = _uiState.update { it.copy(category = category, expandedFaqId = null) }

    fun toggle(item: FaqItem) {
        val expanding = _uiState.value.expandedFaqId != item.id
        _uiState.update { it.copy(expandedFaqId = if (expanding) item.id else null) }
        if (expanding) {
            tracker?.track(
                TrackingPlan.FAQ_VIEWED,
                mapOf("category" to item.category.id, "faqId" to item.id),
                operationKey = "$visitId:${item.id}",
                assignment = assignment
            )
        }
    }

    /** Escenario 2: the visit is recorded as solved without creating a claim. */
    fun markResolved(item: FaqItem) {
        _uiState.update { it.copy(resolvedFaqIds = it.resolvedFaqIds + item.id) }
        tracker?.track(
            TrackingPlan.FAQ_RESOLVED,
            mapOf("category" to item.category.id, "faqId" to item.id),
            operationKey = "$visitId:${item.id}",
            assignment = assignment
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SupportViewModel() as T
        }
    }
}
