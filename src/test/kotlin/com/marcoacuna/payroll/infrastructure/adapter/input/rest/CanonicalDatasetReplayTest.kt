package com.marcoacuna.payroll.infrastructure.adapter.input.rest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.HttpClientErrorException
import java.io.File
import java.math.BigDecimal
import kotlin.test.BeforeTest

/**
 * Replay runner: reads all test cases from the canonical dataset
 * (qa/canonical_test_cases.json) and executes them against the live API.
 *
 * Test types:
 * - exact_match: assert every non-null expected field for exact equality
 * - sv_formula: assert SV deductions (rv, kv, pv, av) + structure; skip PAP nulls
 * - error_contract: assert HTTP status, error code, error field
 * - trace_contract: assert trace structure (steps count, fields present)
 * - oracle_pending: assert structure only; PAP values need BMF verification
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CanonicalDatasetReplayTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var restClient: RestClient

    @BeforeTest
    fun setup() {
        restClient = RestClient.builder().baseUrl("http://localhost:$port").build()
    }

    companion object {
        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        private val dataset: TestDataset by lazy {
            val file = File("qa/canonical_test_cases.json")
            require(file.exists()) { "Canonical dataset not found at ${file.absolutePath}" }
            mapper.readValue<TestDataset>(file.readText())
        }

        @JvmStatic
        fun successCases(): List<Arguments> =
            dataset.cases
                .filter { it.expected.shouldSucceed }
                .map { Arguments.of(it.id, it) }

        @JvmStatic
        fun errorCases(): List<Arguments> =
            dataset.cases
                .filter { !it.expected.shouldSucceed }
                .map { Arguments.of(it.id, it) }
    }

    @ParameterizedTest(name = "[{0}] {1}")
    @MethodSource("successCases")
    fun `replay success case`(id: String, case: TestCase) {
        val requestBody = buildRequestJson(case)

        val response = restClient.post()
            .uri("/api/v1/payroll/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .toEntity(String::class.java)

        assertThat(response.statusCode)
            .describedAs("$id: expected HTTP 200")
            .isEqualTo(HttpStatus.OK)

        val body = mapper.readTree(response.body!!)

        val softly = SoftAssertions()

        // calculationMode
        case.expected.calculationMode?.let {
            softly.assertThat(body.path("calculationMode").asText())
                .describedAs("$id: calculationMode")
                .isEqualTo(it)
        }

        // papVersion
        case.expected.papVersion?.let {
            softly.assertThat(body.path("trace").path("papVersion").asText())
                .describedAs("$id: papVersion")
                .isEqualTo(it)
        }

        // Deductions — only assert non-null fields
        val deductionMap = buildDeductionMap(body)
        case.expected.deductions?.let { expected ->
            assertDeduction(softly, id, "rv", expected.rv, deductionMap["RENTENVERSICHERUNG"])
            assertDeduction(softly, id, "kv", expected.kv, deductionMap["KRANKENVERSICHERUNG"])
            assertDeduction(softly, id, "pv", expected.pv, deductionMap["PFLEGEVERSICHERUNG"])
            assertDeduction(softly, id, "av", expected.av, deductionMap["ARBEITSLOSENVERSICHERUNG"])
            assertDeduction(softly, id, "lohnsteuer", expected.lohnsteuer, deductionMap["LOHNSTEUER"])
            assertDeduction(softly, id, "soli", expected.soli, deductionMap["SOLIDARITAETSZUSCHLAG"])
            assertDeduction(softly, id, "kirchensteuer", expected.kirchensteuer, deductionMap["KIRCHENSTEUER"])
        }

        // Totals
        case.expected.totalDeductions?.let {
            softly.assertThat(body.path("totalDeductions").decimalValue())
                .describedAs("$id: totalDeductions")
                .isEqualByComparingTo(it)
        }
        case.expected.netSalary?.let {
            softly.assertThat(body.path("netSalary").decimalValue())
                .describedAs("$id: netSalary")
                .isEqualByComparingTo(it)
        }

        // Trace assertions
        case.traceAssertions?.let { ta ->
            val trace = body.path("trace")
            ta.stepsCount?.let {
                softly.assertThat(trace.path("steps").size())
                    .describedAs("$id: trace steps count")
                    .isEqualTo(it)
            }
            ta.isUebergangsbereich?.let {
                softly.assertThat(trace.path("uebergangsbereich").asBoolean())
                    .describedAs("$id: isUebergangsbereich")
                    .isEqualTo(it)
            }
            ta.engineVersion?.let {
                softly.assertThat(trace.path("engineVersion").asText())
                    .describedAs("$id: engineVersion")
                    .isEqualTo(it)
            }
            ta.bpiSV?.let {
                softly.assertThat(trace.path("bpiSV").decimalValue())
                    .describedAs("$id: bpiSV")
                    .isEqualByComparingTo(it)
            }
        }

        // Structure checks for all success cases
        softly.assertThat(body.path("deductions").size())
            .describedAs("$id: should have 7 deductions")
            .isEqualTo(7)
        softly.assertThat(body.path("trace").path("traceId").asText())
            .describedAs("$id: traceId should be present")
            .isNotBlank()

        softly.assertAll()
    }

    @ParameterizedTest(name = "[{0}] {1}")
    @MethodSource("errorCases")
    fun `replay error case`(id: String, case: TestCase) {
        val requestBody = buildRequestJson(case)
        val expectedStatus = HttpStatus.valueOf(case.expected.httpStatus)

        try {
            restClient.post()
                .uri("/api/v1/payroll/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toEntity(String::class.java)

            // If we reach here, the request succeeded unexpectedly
            assertThat(false)
                .describedAs("$id: expected HTTP ${case.expected.httpStatus} but got 200")
                .isTrue()
        } catch (e: HttpClientErrorException) {
            assertThat(e.statusCode)
                .describedAs("$id: HTTP status")
                .isEqualTo(expectedStatus)

            val errorBody = mapper.readTree(e.responseBodyAsString)

            case.expected.errorCode?.let {
                assertThat(errorBody.path("code").asText())
                    .describedAs("$id: error code")
                    .isEqualTo(it)
            }

            // errorField: null means field should be absent/null in response
            if (case.expected.errorField != null) {
                assertThat(errorBody.path("field").asText())
                    .describedAs("$id: error field")
                    .isEqualTo(case.expected.errorField)
            } else {
                val fieldNode = errorBody.path("field")
                assertThat(fieldNode.isNull || fieldNode.isMissingNode)
                    .describedAs("$id: error field should be null, was '${fieldNode.asText()}'")
                    .isTrue()
            }
        }
    }

    // --- Helpers ---

    private fun buildRequestJson(case: TestCase): String {
        val input = case.input
        val fields = mutableMapOf<String, Any?>(
            "grossMonthlySalary" to input.grossMonthlySalary,
            "stkl" to input.stkl,
            "af" to input.af,
            "zkf" to input.zkf,
            "lzzfreib" to input.lzzfreib,
            "lzzhinzu" to input.lzzhinzu,
            "r" to input.r,
            "kvz" to input.kvz,
            "krv" to input.krv,
            "pkv" to input.pkv,
            "pkpv" to input.pkpv,
            "pvs" to input.pvs,
            "pvz" to input.pvz,
            "federalState" to input.federalState,
            "numberOfChildrenUnder25" to input.numberOfChildrenUnder25,
            "birthDate" to input.birthDate,
            "calculationDate" to case.calculationDate
        )
        if (input.f != null) {
            fields["f"] = input.f
        }
        return mapper.writeValueAsString(fields)
    }

    private fun buildDeductionMap(body: JsonNode): Map<String, BigDecimal> {
        val map = mutableMapOf<String, BigDecimal>()
        body.path("deductions").forEach { d ->
            map[d.path("type").asText()] = d.path("amount").decimalValue()
        }
        return map
    }

    private fun assertDeduction(
        softly: SoftAssertions,
        id: String,
        name: String,
        expected: BigDecimal?,
        actual: BigDecimal?
    ) {
        if (expected != null && actual != null) {
            softly.assertThat(actual)
                .describedAs("$id: $name")
                .isEqualByComparingTo(expected)
        }
    }
}

// --- Data classes for canonical dataset ---

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestDataset(
    val metadata: TestMetadata,
    val cases: List<TestCase>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestMetadata(
    val totalCases: Int,
    val engine: String,
    val engineVersion: String? = null,
    val generatedAt: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestCase(
    val id: String,
    val title: String,
    val category: String,
    val testType: String,
    val calculationDate: String,
    val input: TestInput,
    val expected: TestExpected,
    val traceAssertions: TestTraceAssertions? = null,
    val notes: String? = null
) {
    override fun toString(): String = title
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestInput(
    val grossMonthlySalary: BigDecimal,
    val stkl: Int,
    val af: Boolean = false,
    val f: BigDecimal? = null,
    val zkf: BigDecimal = BigDecimal.ZERO,
    val lzzfreib: BigDecimal = BigDecimal.ZERO,
    val lzzhinzu: BigDecimal = BigDecimal.ZERO,
    val r: Int = 0,
    val kvz: BigDecimal = BigDecimal("2.50"),
    val krv: Int = 0,
    val pkv: Int = 0,
    val pkpv: BigDecimal = BigDecimal.ZERO,
    val pvs: Boolean = false,
    val pvz: Boolean = false,
    val federalState: String = "BAYERN",
    val numberOfChildrenUnder25: Int = 0,
    val birthDate: String = "1990-05-15"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestExpected(
    val shouldSucceed: Boolean,
    val httpStatus: Int,
    val papVersion: String? = null,
    val calculationMode: String? = null,
    val deductions: TestDeductions? = null,
    val totalDeductions: BigDecimal? = null,
    val netSalary: BigDecimal? = null,
    val errorCode: String? = null,
    val errorField: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestDeductions(
    val rv: BigDecimal? = null,
    val kv: BigDecimal? = null,
    val pv: BigDecimal? = null,
    val av: BigDecimal? = null,
    val lohnsteuer: BigDecimal? = null,
    val soli: BigDecimal? = null,
    val kirchensteuer: BigDecimal? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestTraceAssertions(
    val isUebergangsbereich: Boolean? = null,
    val stepsCount: Int? = null,
    val engineVersion: String? = null,
    val bpiSV: BigDecimal? = null
)
