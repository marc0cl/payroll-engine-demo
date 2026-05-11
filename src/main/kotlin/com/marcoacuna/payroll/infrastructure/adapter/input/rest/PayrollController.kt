package com.marcoacuna.payroll.infrastructure.adapter.input.rest

import com.marcoacuna.payroll.application.usecase.CalculateNetSalaryUseCase
import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.infrastructure.adapter.input.rest.dto.CalculationRequest
import com.marcoacuna.payroll.infrastructure.adapter.input.rest.dto.CalculationResponse
import com.marcoacuna.payroll.infrastructure.metrics.PayrollMetrics
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payroll")
@Tag(name = "Payroll", description = "Net salary calculation (PAP LZZ=2 subset)")
class PayrollController(
    private val calculateNetSalaryUseCase: CalculateNetSalaryUseCase,
    private val metrics: PayrollMetrics
) {

    @PostMapping("/calculate")
    @Operation(
        summary = "Calculate net salary from gross",
        description = "Applies German payroll deductions using the official BMF PAP for monthly salary (LZZ=2). " +
            "Returns full deduction breakdown, net salary, and auditable calculation trace."
    )
    fun calculate(@Valid @RequestBody request: CalculationRequest): ResponseEntity<Any> {
        metrics.incrementCalculations()
        val startTime = System.nanoTime()

        return try {
            val result = calculateNetSalaryUseCase.execute(request.toCommand())
            val duration = (System.nanoTime() - startTime) / 1_000_000.0
            metrics.recordDuration(duration)

            when (result) {
                is DomainResult.Success -> ResponseEntity.ok(CalculationResponse.from(result.value))
                is DomainResult.Failure -> throw DomainErrorException(result.error)
            }
        } catch (e: DomainErrorException) {
            metrics.incrementErrors()
            throw e
        }
    }
}
