package com.incleanhome.mobile

import com.google.gson.Gson
import com.incleanhome.mobile.booking.data.*
import com.incleanhome.mobile.events.data.*
import com.incleanhome.mobile.iam.data.*
import com.incleanhome.mobile.messaging.data.SendMessageRequest
import com.incleanhome.mobile.reviews.data.CreateReviewRequest
import com.incleanhome.mobile.reviews.presentation.CreateReviewViewModel
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CoreContractsTest {
    private val gson = Gson()

    @Test fun loginResponsesSelectRequiredStepsAndRejectMissingChallenge() {
        assertEquals(LoginNextStep.TERMS, (interpretAuthResponse(AuthResponse(requiresTermsAcceptance=true, challengeToken="c")) as LoginResult.Challenge).nextStep)
        assertEquals(LoginNextStep.TWO_FA_SETUP, (interpretAuthResponse(AuthResponse(requires2faSetup=true, challengeToken="c")) as LoginResult.Challenge).nextStep)
        assertEquals(LoginNextStep.TWO_FA_VERIFY, (interpretAuthResponse(AuthResponse(requires2fa=true, challengeToken="c")) as LoginResult.Challenge).nextStep)
        assertTrue(interpretAuthResponse(AuthResponse(requires2fa=true)) is LoginResult.Error)
    }

    @Test fun authenticatedResponseRequiresUserAndToken() {
        val user = AuthUser(1,"a@b.com","client","A",null)
        assertTrue(interpretAuthResponse(AuthResponse(user=user,token="jwt")) is LoginResult.Authenticated)
        assertTrue(interpretAuthResponse(AuthResponse(user=user)) is LoginResult.Error)
    }

    @Test fun bookingStatusesAndCancellationPayloadMatchBackend() {
        assertEquals(setOf("pending","accepted","rejected","cancelled","completed"), setOf(BookingStatus.PENDING,BookingStatus.ACCEPTED,BookingStatus.REJECTED,BookingStatus.CANCELLED,BookingStatus.COMPLETED))
        assertEquals("{\"status\":\"cancelled\"}", gson.toJson(UpdateBookingStatusRequest(BookingStatus.CANCELLED)))
    }

    @Test fun reviewConstraintsAreOneToFiveAndCommentIsBounded() {
        assertEquals(1, CreateReviewViewModel.MIN_RATING); assertEquals(5, CreateReviewViewModel.MAX_RATING)
        assertEquals(1000, CreateReviewViewModel.MAX_COMMENT_LENGTH)
        val json = gson.toJson(CreateReviewRequest(4,2,5,"ok"))
        assertTrue(json.contains("\"bookingId\":4") && json.contains("\"rating\":5"))
    }

    @Test fun eventAndMessagingContractsSerializeExpectedFields() {
        val event = CreateEventRequest("t","d",listOf("cleaning"),"zone","addr","2026-01-01","09:00","10:00",BigDecimal("1"),1,BigDecimal("10"),"2025-12-01")
        assertTrue(gson.toJson(event).contains("\"workersNeeded\":1"))
        assertEquals("{\"content\":\"hello\"}", gson.toJson(SendMessageRequest("hello")))
        assertEquals(setOf("pending","accepted","rejected","withdrawn"), setOf(ApplicationStatus.PENDING,ApplicationStatus.ACCEPTED,ApplicationStatus.REJECTED,ApplicationStatus.WITHDRAWN))
    }
}
