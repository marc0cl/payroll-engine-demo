package com.marcoacuna.payroll.infrastructure.adapter.input.rest.dto

import com.marcoacuna.payroll.domain.model.PayrollResult
import java.math.BigDecimal
import java.time.LocalDate

data class CalculationResponse(
    val grossSalary: BigDecimal,
    val deductions: List<DeductionDto>,
    val totalDeductions: BigDecimal,
    val netSalary: BigDecimal,
    val effectiveWithholdingRate: BigDecimal,
    val calculationMode: String,
    val trace: TraceDto
) {
    companion object {
        fun from(result: PayrollResult): CalculationResponse = CalculationResponse(
            grossSalary = result.grossSalary.amount,
            deductions = result.deductions.map { d ->
                DeductionDto(
                    type = d.type.name,
                    germanName = d.type.germanName,
                    calculationBase = d.calculationBase,
                    rate = d.rate,
                    amount = d.amount,
                    legalReference = d.legalReference
                )
            },
            totalDeductions = result.totalDeductions,
            netSalary = result.netSalary.amount,
            effectiveWithholdingRate = result.effectiveWithholdingRate,
            calculationMode = result.calculationMode.name,
            trace = TraceDto(
                traceId = result.trace.traceId.toString(),
                timestamp = result.trace.timestamp.toString(),
                papVersion = result.trace.papVersion,
                ratesEffectiveFrom = result.trace.ratesEffectiveFrom,
                engineVersion = result.trace.engineVersion,
                bpiSV = result.trace.bpiSV,
                isUebergangsbereich = result.trace.isUebergangsbereich,
                capsApplied = result.trace.capsApplied,
                steps = result.trace.steps.map { s ->
                    TraceStepDto(
                        deductionType = s.deductionType.name,
                        calculationBase = s.calculationBase,
                        appliedRate = s.appliedRate,
                        amount = s.amount,
                        legalReference = s.legalReference,
                        roundingInfo = s.roundingInfo,
                        notes = s.notes
                    )
                }
            )
        )
    }
}

data class DeductionDto(
    val type: String,
    val germanName: String,
    val calculationBase: BigDecimal,
    val rate: BigDecimal,
    val amount: BigDecimal,
    val legalReference: String
)

data class TraceDto(
    val traceId: String,
    val timestamp: String,
    val papVersion: String,
    val ratesEffectiveFrom: LocalDate,
    val engineVersion: String,
    val bpiSV: BigDecimal,
    val isUebergangsbereich: Boolean,
    val capsApplied: List<String>,
    val steps: List<TraceStepDto>
)

data class TraceStepDto(
    val deductionType: String,
    val calculationBase: BigDecimal,
    val appliedRate: BigDecimal,
    val amount: BigDecimal,
    val legalReference: String,
    val roundingInfo: String,
    val notes: String?
)
