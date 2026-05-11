package com.marcoacuna.payroll.domain.service

import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.domain.audit.CalculationTrace
import com.marcoacuna.payroll.domain.calculation.*
import com.marcoacuna.payroll.domain.error.PayrollError
import com.marcoacuna.payroll.domain.model.*
import com.marcoacuna.payroll.domain.rates.EffectiveRates
import com.marcoacuna.payroll.domain.rates.RatesProvider
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/**
 * Central orchestrator for the payroll calculation pipeline.
 *
 * Pipeline order (respects dependency chain):
 * 1. Input validation (pre-PAP plausibility checks)
 * 2. Rate lookup by effective date
 * 3. Uebergangsbereich determination (sets bpiSV)
 * 4. Social insurance: RV, KV, PV, AV (independent, operate on bpiSV)
 * 5. PAP: Lohnsteuer + Soli + KiSt-Basis (uses SV results for Vorsorgepauschale)
 * 6. Kirchensteuer (uses BK from PAP)
 * 7. Assemble result with trace
 */
class PayrollCalculationService(
    private val ratesProvider: RatesProvider
) {

    companion object {
        const val ENGINE_VERSION = "1.0.0"
    }

    private val uebergangsbereichCalculator = UebergangsbereichCalculator()
    private val pensionCalculator = PensionCalculator()
    private val healthInsuranceCalculator = HealthInsuranceCalculator()
    private val careInsuranceCalculator = CareInsuranceCalculator()
    private val unemploymentCalculator = UnemploymentCalculator()
    private val papCalculator = PapCalculator()
    private val churchTaxCalculator = ChurchTaxCalculator()

    fun calculate(input: EmployeeInput): DomainResult<PayrollResult> {
        // Step 1: Validate input
        val validationResult = InputValidator.validate(input)
        if (validationResult is DomainResult.Failure) {
            return validationResult as DomainResult<PayrollResult>
        }

        // Step 2: Look up rates
        val rates = ratesProvider.ratesFor(input.calculationDate)
            ?: return DomainResult.Failure(
                PayrollError.UnsupportedCalculationPeriod(
                    input.calculationDate,
                    ratesProvider.allPeriods().map { "${it.effectiveFrom} - ${it.effectiveUntil ?: "current"}" }
                )
            )

        // Step 3: Build context
        val context = CalculationContext(input = input, rates = rates)

        // Step 4: Uebergangsbereich
        uebergangsbereichCalculator.calculate(context)

        // Step 5: Social insurance (all operate on context.bpiSV)
        pensionCalculator.calculate(context)
        healthInsuranceCalculator.calculate(context)
        careInsuranceCalculator.calculate(context)
        unemploymentCalculator.calculate(context)

        // Step 6: PAP (Lohnsteuer + Soli + KiSt-Basis, uses SV results)
        papCalculator.calculate(context)

        // Step 7: Kirchensteuer
        churchTaxCalculator.calculate(context)

        // Assemble result
        val totalDeductions = context.deductions
            .sumOf { it.amount }
            .setScale(2, RoundingMode.HALF_UP)

        val netAmount = input.grossMonthlySalary.amount
            .subtract(totalDeductions)
            .setScale(2, RoundingMode.HALF_UP)

        val effectiveRate = if (input.grossMonthlySalary.amount > BigDecimal.ZERO) {
            totalDeductions.divide(input.grossMonthlySalary.amount, 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val trace = CalculationTrace(
            traceId = UUID.randomUUID(),
            timestamp = Instant.now(),
            calculationMode = CalculationMode.OFFICIAL_PAP_LZZ2,
            papVersion = rates.papVersion,
            ratesEffectiveFrom = rates.effectiveFrom,
            engineVersion = ENGINE_VERSION,
            bpiSV = context.bpiSV,
            isUebergangsbereich = context.isUebergangsbereich,
            capsApplied = context.capsApplied.toList(),
            steps = context.steps.toList()
        )

        return DomainResult.Success(
            PayrollResult(
                grossSalary = input.grossMonthlySalary,
                deductions = context.deductions.toList(),
                totalDeductions = totalDeductions,
                netSalary = NetSalary.of(netAmount),
                effectiveWithholdingRate = effectiveRate,
                calculationMode = CalculationMode.OFFICIAL_PAP_LZZ2,
                trace = trace
            )
        )
    }
}
