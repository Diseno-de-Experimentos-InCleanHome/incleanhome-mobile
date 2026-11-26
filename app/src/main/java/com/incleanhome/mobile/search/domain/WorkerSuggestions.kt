package com.incleanhome.mobile.search.domain

import com.incleanhome.mobile.search.data.AvailabilitySlot
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.search.data.WorkerRepository
import com.incleanhome.mobile.search.data.WorkerResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class SuggestionRequest(
    val serviceType: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val district: String
)

/**
 * Suggestion of US33 built only from published API data: workers that offer the service in the
 * requested district (US06 filters) and whose availability (US07) covers the requested schedule,
 * ordered by rating and completed services, up to five.
 *
 * Limitation: the API has no service area yet (US32), so proximity is the district match and the
 * order cannot use distance. When GET workers/suggestions exists it replaces this composition.
 */
class WorkerSuggestionService(
    private val repository: WorkerRepository = WorkerRepository(),
    private val maxCandidates: Int = 20
) {
    suspend fun suggest(request: SuggestionRequest): WorkerResult<List<Worker>> {
        val candidates = when (val result = repository.searchWorkers(request.serviceType, request.district)) {
            // Rank first so the availability check covers the best rated workers.
            is WorkerResult.Success -> rank(result.data, limit = maxCandidates)
            is WorkerResult.Error -> return result
        }
        val available = coroutineScope {
            candidates.map { worker ->
                async {
                    when (val slots = repository.getAvailability(worker.id)) {
                        is WorkerResult.Success -> worker.takeIf { isAvailable(slots.data, request) }
                        is WorkerResult.Error -> null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        return WorkerResult.Success(rank(available))
    }

    companion object {
        const val MAX_SUGGESTIONS = 5

        /** Backend dayOfWeek: 0 = Sunday … 6 = Saturday (System.DayOfWeek). */
        fun backendDayOfWeek(date: LocalDate): Int = date.dayOfWeek.value % 7

        fun isAvailable(slots: List<AvailabilitySlot>, request: SuggestionRequest): Boolean {
            if (!request.end.isAfter(request.start)) return false
            val day = backendDayOfWeek(request.date)
            return slots.any { slot ->
                val start = parse(slot.startTime)
                val end = parse(slot.endTime)
                slot.isAvailable && slot.dayOfWeek == day && start != null && end != null &&
                    !start.isAfter(request.start) && !end.isBefore(request.end)
            }
        }

        fun rank(workers: List<Worker>, limit: Int = MAX_SUGGESTIONS): List<Worker> = workers
            .sortedWith(compareByDescending<Worker> { it.averageRating }.thenByDescending { it.totalServices })
            .take(limit)

        private fun parse(value: String): LocalTime? = try {
            LocalTime.parse(value.take(5))
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
