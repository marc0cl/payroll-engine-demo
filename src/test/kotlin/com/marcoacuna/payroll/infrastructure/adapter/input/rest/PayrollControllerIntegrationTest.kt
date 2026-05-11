package com.marcoacuna.payroll.infrastructure.adapter.input.rest

import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import kotlin.test.BeforeTest
import kotlin.test.Test

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PayrollControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var restClient: RestClient

    @BeforeTest
    fun setup() {
        restClient = RestClient.builder().baseUrl("http://localhost:$port").build()
    }

    private val validRequestBody = """
        {
            "grossMonthlySalary": 4500.00,
            "stkl": 1,
            "af": false,
            "zkf": 0.0,
            "lzzfreib": 0,
            "lzzhinzu": 0,
            "r": 1,
            "kvz": 2.50,
            "krv": 0,
            "pkv": 0,
            "pkpv": 0,
            "pvs": false,
            "pvz": true,
            "federalState": "BAYERN",
            "numberOfChildrenUnder25": 0,
            "birthDate": "1990-05-15",
            "calculationDate": "2025-06-15"
        }
    """.trimIndent()

    @Test
    fun `should return 200 with valid request`() {
        val response = restClient.post()
            .uri("/api/v1/payroll/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequestBody)
            .retrieve()
            .toEntity(String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("OFFICIAL_PAP_LZZ2")
        assertThat(response.body).contains("deductions")
        assertThat(response.body).contains("netSalary")
        assertThat(response.body).contains("trace")
    }

    @Test
    fun `should return 400 for negative salary`() {
        val body = validRequestBody.replace("4500.00", "-100.00")
        try {
            restClient.post()
                .uri("/api/v1/payroll/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String::class.java)
            assertThat(false).describedAs("Expected 4xx error").isTrue()
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            assertThat(e.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(e.responseBodyAsString).contains("INVALID_GROSS_SALARY")
        }
    }

    @Test
    fun `should include correlation ID in response headers`() {
        val response = restClient.post()
            .uri("/api/v1/payroll/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Correlation-Id", "test-correlation-123")
            .body(validRequestBody)
            .retrieve()
            .toEntity(String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.headers["X-Correlation-Id"]).contains("test-correlation-123")
    }

    @Test
    fun `should generate correlation ID if not provided`() {
        val response = restClient.post()
            .uri("/api/v1/payroll/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .body(validRequestBody)
            .retrieve()
            .toEntity(String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.headers["X-Correlation-Id"]).isNotNull()
    }

    @Test
    fun `should return 404 for unsupported calculation date`() {
        val body = validRequestBody.replace("2025-06-15", "2020-01-01")
        try {
            restClient.post()
                .uri("/api/v1/payroll/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String::class.java)
            assertThat(false).describedAs("Expected 4xx error").isTrue()
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            assertThat(e.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(e.responseBodyAsString).contains("UNSUPPORTED_PERIOD")
        }
    }
}
