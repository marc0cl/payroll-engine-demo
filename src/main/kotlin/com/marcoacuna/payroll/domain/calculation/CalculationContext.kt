package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.audit.CalculationStep
import com.marcoacuna.payroll.domain.model.Deduction
import com.marcoacuna.payroll.domain.model.EmployeeInput
import com.marcoacuna.payroll.domain.rates.EffectiveRates
import java.math.BigDecimal

/**
 * Mutable context threaded through the calculation pipeline.
 * Accumulates intermediate results that downstream calculators depend on:
 * - SV contributions feed into PAP Vorsorgepauschale
 * - PAP Lohnsteuer feeds into Soli and KiSt
 */
data class CalculationContext(
    val input: EmployeeInput,
    val rates: EffectiveRates,

    /** Beitragspflichtige Einnahme for SV — may be reduced in Uebergangsbereich. */
    var bpiSV: BigDecimal = input.grossMonthlySalary.amount,

    /** Whether this employee falls in the Uebergangsbereich (Midijob zone). */
    var isUebergangsbereich: Boolean = false,

    // --- SV results (needed by PAP for Vorsorgepauschale) ---
    var pensionContribution: BigDecimal = BigDecimal.ZERO,
    var healthInsuranceContribution: BigDecimal = BigDecimal.ZERO,
    var careInsuranceContribution: BigDecimal = BigDecimal.ZERO,
    var unemploymentContribution: BigDecimal = BigDecimal.ZERO,

    // --- PAP results (needed by KiSt calculator) ---
    /** LSTLZZ: Lohnsteuer for the Lohnzahlungszeitraum (monthly). */
    var lohnsteuer: BigDecimal = BigDecimal.ZERO,
    /** SOLZLZZ: Solidaritaetszuschlag for the period. */
    var solidaritaetszuschlag: BigDecimal = BigDecimal.ZERO,
    /** BK: Bemessungsgrundlage for Kirchensteuer (from PAP). */
    var kirchensteuerBasis: BigDecimal = BigDecimal.ZERO,

    // --- Audit trail ---
    val deductions: MutableList<Deduction> = mutableListOf(),
    val steps: MutableList<CalculationStep> = mutableListOf(),
    val capsApplied: MutableList<String> = mutableListOf()
)
