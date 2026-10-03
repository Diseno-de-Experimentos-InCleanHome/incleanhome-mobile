package com.incleanhome.mobile

import com.incleanhome.mobile.reviews.data.CreateReviewRequest
import com.incleanhome.mobile.reviews.data.Review
import com.incleanhome.mobile.reviews.data.ReviewResult
import com.incleanhome.mobile.reviews.data.ReviewsApi
import com.incleanhome.mobile.reviews.data.ReviewsRepository
import com.incleanhome.mobile.reviews.presentation.CreateReviewViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewsRepositoryTest {

    /* USER STORY 13 - Calificar servicio completado */
    @Test
    fun testCreateReview_EnviaCalificacionYComentario() = runBlocking {
        //Arrange
        val api = FakeReviewsApi()
        val repository = ReviewsRepository(api)
        val request = CreateReviewRequest(
            bookingId = 15,
            workerId = 20,
            rating = 5,
            comment = "Excelente servicio"
        )
        api.review = review()

        //Act
        val resultado = repository.createReview(request)

        //Assert
        assertEquals(request, api.lastRequest)
        assertTrue(resultado is ReviewResult.Success)
        resultado as ReviewResult.Success
        assertEquals(5, resultado.data.rating)
    }

    /* USER STORY 13 - Rango permitido de estrellas */
    @Test
    fun testRating_LimitesPermitidosSonUnoYCinco() {
        //Arrange
        val minimoEsperado = 1
        val maximoEsperado = 5

        //Act
        val minimo = CreateReviewViewModel.MIN_RATING
        val maximo = CreateReviewViewModel.MAX_RATING

        //Assert
        assertEquals(minimoEsperado, minimo)
        assertEquals(maximoEsperado, maximo)
    }

    private fun review() = Review(
        id = 3,
        bookingId = 15,
        clientId = 4,
        workerId = 20,
        clientName = "Cliente",
        rating = 5,
        comment = "Excelente servicio",
        createdAt = "2026-10-03T15:00:00Z"
    )

    private class FakeReviewsApi : ReviewsApi {
        var review = Review(1, 1, 1, 1, "", 1, "", null)
        var lastRequest: CreateReviewRequest? = null

        override suspend fun createReview(request: CreateReviewRequest): Review {
            lastRequest = request
            return review
        }

        override suspend fun getWorkerReviews(workerId: Int): List<Review> = listOf(review)
    }
}
