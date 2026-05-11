package com.marcoacuna.payroll.infrastructure.adapter.input.rest

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient
import kotlin.test.BeforeTest
import kotlin.test.Test

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RatesControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var restClient: RestClient

    @BeforeTest
    fun setup() {
        restClient = RestClient.builder().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `should return rates for supported date`() {
        val response = restClient.get()
            .uri("/api/v1/rates?date=2025-06-15")
            .retrieve()
            .toEntity(String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("PAP2025_Maerz")
    }

    @Test
    fun `should return all periods when no date specified`() {
        val response = restClient.get()
            .uri("/api/v1/rates")
            .retrieve()
            .toEntity(String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("PAP2025_Januar")
        assertThat(response.body).contains("PAP2025_Maerz")
        assertThat(response.body).contains("PAP2026")
    }

    @Test
    fun `should return 404 for unsupported date`() {
        try {
            restClient.get()
                .uri("/api/v1/rates?date=2020-01-01")
                .retrieve()
                .toEntity(String::class.java)
            assertThat(false).describedAs("Expected 4xx error").isTrue()
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            assertThat(e.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(e.responseBodyAsString).contains("UNSUPPORTED_PERIOD")
        }
    }

    @Test
    fun `should distinguish PAP versions within same year`() {
        val jan = restClient.get()
            .uri("/api/v1/rates?date=2025-01-15")
            .retrieve()
            .toEntity(String::class.java)
        assertThat(jan.body).contains("PAP2025_Januar")

        val jun = restClient.get()
            .uri("/api/v1/rates?date=2025-06-15")
            .retrieve()
            .toEntity(String::class.java)
        assertThat(jun.body).contains("PAP2025_Maerz")
    }
}
