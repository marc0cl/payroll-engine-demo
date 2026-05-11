package com.marcoacuna.payroll.infrastructure.adapter.input.rest

import com.marcoacuna.payroll.application.port.input.RatesQuery
import com.marcoacuna.payroll.application.usecase.QueryRatesUseCase
import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.infrastructure.adapter.input.rest.dto.RatesResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/rates")
@Tag(name = "Rates", description = "Tax and social insurance rates by effective date")
class RatesController(
    private val queryRatesUseCase: QueryRatesUseCase
) {

    @GetMapping
    @Operation(summary = "Query rates", description = "Returns rates for a specific date, or all available periods if no date is provided.")
    fun getRates(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<Any> {
        if (date != null) {
            val result = queryRatesUseCase.queryByDate(RatesQuery(date))
            return when (result) {
                is DomainResult.Success -> ResponseEntity.ok(RatesResponse.from(result.value))
                is DomainResult.Failure -> throw DomainErrorException(result.error)
            }
        }
        val all = queryRatesUseCase.queryAll().map { RatesResponse.from(it) }
        return ResponseEntity.ok(all)
    }
}
