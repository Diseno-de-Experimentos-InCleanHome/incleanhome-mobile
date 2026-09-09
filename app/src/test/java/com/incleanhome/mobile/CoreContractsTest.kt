package com.incleanhome.mobile

import com.google.gson.Gson
import com.incleanhome.mobile.booking.data.*
import com.incleanhome.mobile.events.data.*
import com.incleanhome.mobile.iam.data.*
import com.incleanhome.mobile.messaging.data.SendMessageRequest
import com.incleanhome.mobile.iam.presentation.TERMS_VERSION
import com.incleanhome.mobile.legal.presentation.LegalBlock
import com.incleanhome.mobile.legal.presentation.parseLegalMarkdown
import com.incleanhome.mobile.navigation.MobileAccess
import com.incleanhome.mobile.navigation.PRIVACY_DOCUMENT_ROUTE
import com.incleanhome.mobile.navigation.TERMS_DOCUMENT_ROUTE
import com.incleanhome.mobile.navigation.mobileAccessForRole
import com.incleanhome.mobile.reviews.data.CreateReviewRequest
import com.incleanhome.mobile.reviews.presentation.CreateReviewViewModel
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import javax.xml.parsers.DocumentBuilderFactory

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

    @Test fun currentTermsVersionIsV3() {
        assertEquals("v3", TERMS_VERSION)
        assertEquals("v3", RegisterClientRequest("A", "a@b.com", "secret", null, TERMS_VERSION).acceptedTermsVersion)
        assertEquals(
            "v3",
            RegisterWorkerRequest(
                "W", "w@b.com", "secret", null, 30, AuthGender.FEMALE,
                listOf("cleaning"), listOf("Lima"), BigDecimal("20"), 2, null, TERMS_VERSION
            ).acceptedTermsVersion
        )
        assertEquals("v3", AcceptTermsRequest(TERMS_VERSION).version)
        assertEquals("{\"version\":\"v3\"}", gson.toJson(AcceptTermsRequest(TERMS_VERSION)))
    }

    @Test fun legalDocumentsAndRoutesArePackaged() {
        assertTrue(R.raw.terms_v3 != 0)
        assertTrue(R.raw.privacy_v3 != 0)
        assertEquals("terms_document", TERMS_DOCUMENT_ROUTE)
        assertEquals("privacy_document", PRIVACY_DOCUMENT_ROUTE)
    }

    @Test fun packagedLegalDocumentsMatchOfficialSourcesByteForByte() {
        assertArrayEquals(
            projectFile("legal-source/terms-v3.md").readBytes(),
            projectFile("app/src/main/res/raw/terms_v3.md", "src/main/res/raw/terms_v3.md").readBytes()
        )
        assertArrayEquals(
            projectFile("legal-source/privacy-v3.md").readBytes(),
            projectFile("app/src/main/res/raw/privacy_v3.md", "src/main/res/raw/privacy_v3.md").readBytes()
        )
    }

    @Test fun legalMarkdownParserHandlesRequiredDocumentStructure() {
        val blocks = parseLegalMarkdown(
            "# Title\n\n## Section\n\nA **legal** paragraph.\n\n- First item\n  continued\n\n---"
        )
        assertEquals(LegalBlock.Heading(1, "Title"), blocks[0])
        assertEquals(LegalBlock.Heading(2, "Section"), blocks[1])
        assertEquals(LegalBlock.Paragraph("A legal paragraph."), blocks[2])
        assertEquals(LegalBlock.Bullet("First item continued"), blocks[3])
        assertEquals(LegalBlock.Divider, blocks[4])
    }

    @Test fun legalMarkdownParserPreservesLegalTextAndSectionSymbols() {
        val source = "## Protection\n\nReference **required** by §6 and [Privacy policy](./privacy.md) at `/claims`."
        val visible = parseLegalMarkdown(source).joinToString("\n") { block ->
            when (block) {
                is LegalBlock.Heading -> block.text
                is LegalBlock.Paragraph -> block.text
                is LegalBlock.Bullet -> block.text
                LegalBlock.Divider -> ""
            }
        }
        assertEquals("Protection\nReference required by §6 and Privacy policy at /claims.", visible)
        assertFalse(visible.contains("**"))
        assertFalse(visible.contains('`'))
        assertFalse(visible.contains("]("))
        assertTrue(visible.contains("§6"))
    }

    @Test fun englishLegalInterfaceDoesNotContainMixedLanguageAcceptLabel() {
        val stringsFile = projectFile("app/src/main/res/values/strings.xml", "src/main/res/values/strings.xml")
        val englishStrings = stringsFile.readText(Charsets.UTF_8)
        assertFalse(englishStrings.contains("Accept y continuar"))
        assertTrue(englishStrings.contains("Accept and continue"))
    }

    @Test fun localizedResourceKeysAndPlaceholdersAreCompatible() {
        val english = localizedResources("app/src/main/res/values/strings.xml", "src/main/res/values/strings.xml")
        val spanish = localizedResources(
            "app/src/main/res/values-b+es+419/strings.xml",
            "src/main/res/values-b+es+419/strings.xml"
        )
        assertEquals(english.keys - "app_name", spanish.keys)
        english.keys.intersect(spanish.keys).forEach { key ->
            assertEquals("Placeholder mismatch for $key", placeholders(english.getValue(key)), placeholders(spanish.getValue(key)))
        }
    }

    @Test fun bookingAndLegalResourcesAreNotMixedBetweenLanguages() {
        val english = localizedResources("app/src/main/res/values/strings.xml", "src/main/res/values/strings.xml")
        val spanish = localizedResources(
            "app/src/main/res/values-b+es+419/strings.xml",
            "src/main/res/values-b+es+419/strings.xml"
        )
        assertEquals("View and manage the services you have booked.", english.getValue("bookings_subtitle").single())
        assertEquals(
            "You do not have any bookings\nWhen you book a service, it will appear here.",
            english.getValue("empty_bookings").single()
        )
        assertEquals("Consulta y gestiona los servicios que has reservado.", spanish.getValue("bookings_subtitle").single())
        assertEquals(
            "No tienes reservas\nCuando reserves un servicio, aparecerá aquí.",
            spanish.getValue("empty_bookings").single()
        )
        assertFalse(english.values.flatten().any { text ->
            Regex("\\b(reservas|servicios|cuando|consulta|gestiona|guardar|cancelar|volver|estado|horas|membresía|reclamo|términos|privacidad)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        })
        assertFalse(spanish.values.flatten().any { text ->
            Regex("\\b(bookings|services|save|cancel|status|hours|membership|claim|privacy)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        })
        assertEquals("Official legal document available in Spanish.", english.getValue("legal_spanish_only_notice").single())
        assertEquals("", spanish.getValue("legal_spanish_only_notice").single())
    }

    @Test fun servicePluralsAreFullyLocalized() {
        val english = localizedResources("app/src/main/res/values/strings.xml", "src/main/res/values/strings.xml")
        val spanish = localizedResources(
            "app/src/main/res/values-b+es+419/strings.xml",
            "src/main/res/values-b+es+419/strings.xml"
        )
        assertEquals(listOf("%1\$d service", "%1\$d services"), english.getValue("services_count"))
        assertEquals(listOf("%1\$d servicio", "%1\$d servicios"), spanish.getValue("services_count"))
        assertEquals(listOf("%1\$d service completed", "%1\$d services completed"), english.getValue("services_completed"))
        assertEquals(listOf("%1\$d servicio realizado", "%1\$d servicios realizados"), spanish.getValue("services_completed"))
    }

    @Test fun pendingMembershipCannotBecomeAnAuthenticatedResultEvenWithJwt() {
        val worker = AuthUser(2, "w@b.com", "worker", "W", null)
        val result = interpretAuthResponse(
            AuthResponse(
                user = worker,
                token = "must-not-be-persisted",
                membershipPending = true,
                membershipStatus = "pending",
                whatsappLink = "https://wa.me/example"
            )
        )
        assertTrue(result is LoginResult.MembershipBlocked)
        assertFalse(result is LoginResult.Authenticated)
    }

    @Test fun rejectedWorkerDoesNotRouteToWorkerHome() {
        val result = interpretAuthResponse(AuthResponse(membershipStatus = "rejected"))
        assertTrue(result is LoginResult.MembershipBlocked)
        assertFalse(result is LoginResult.Authenticated)
    }

    @Test fun activeWorkerAndClientKeepExistingSessionBehavior() {
        val worker = AuthUser(2, "w@b.com", "worker", "W", null)
        val client = AuthUser(1, "c@b.com", "client", "C", null)
        assertTrue(
            interpretAuthResponse(
                AuthResponse(user = worker, token = "jwt", membershipStatus = "active")
            ) is LoginResult.Authenticated
        )
        assertTrue(
            interpretAuthResponse(AuthResponse(user = client, token = "jwt")) is LoginResult.Authenticated
        )
        assertEquals(MobileAccess.WORKER, mobileAccessForRole("worker"))
        assertEquals(MobileAccess.CLIENT, mobileAccessForRole("client"))
    }

    @Test fun adminHasDedicatedMobileDestination() {
        assertEquals(MobileAccess.ADMIN, mobileAccessForRole("admin"))
        assertNotEquals(MobileAccess.CLIENT, mobileAccessForRole("admin"))
        assertNotEquals(MobileAccess.WORKER, mobileAccessForRole("admin"))
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

    private fun projectFile(vararg candidates: String): File =
        candidates.asSequence()
            .flatMap { path -> sequenceOf(File(path), File("../$path")) }
            .first { it.isFile }

    private fun localizedResources(vararg candidates: String): Map<String, List<String>> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(projectFile(*candidates))
        val result = linkedMapOf<String, List<String>>()
        val children = document.documentElement.childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node.nodeName == "string") {
                result[node.attributes.getNamedItem("name").nodeValue] = listOf(node.textContent.replace("\\n", "\n"))
            } else if (node.nodeName == "plurals") {
                val values = mutableListOf<String>()
                val items = node.childNodes
                for (itemIndex in 0 until items.length) {
                    val item = items.item(itemIndex)
                    if (item.nodeName == "item") values += item.textContent.replace("\\n", "\n")
                }
                result[node.attributes.getNamedItem("name").nodeValue] = values
            }
        }
        return result
    }

    private fun placeholders(values: List<String>): List<List<String>> = values.map { value ->
        Regex("%(?:\\d+\\$)?[dsf]").findAll(value).map { it.value }.toList()
    }
}
