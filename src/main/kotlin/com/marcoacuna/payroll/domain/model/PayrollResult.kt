package com.marcoacuna.payroll.domain.model

import com.marcoacuna.payroll.domain.audit.CalculationTrace
import java.math.BigDecimal

/**
 * Complete result of a payroll calculation.
 * Contains the full deduction breakdown, net salary, effective withholding rate,
 * the calculation mode used, and an auditable trace of every step.
 */
data class PayrollResult(
    val grossSalary: GrossSalary,
    val deductions: List<Deduction>,
    val totalDeductions: BigDecimal,
    val netSalary: NetSalary,
    val effectiveWithholdingRate: BigDecimal,
    val calculationMode: CalculationMode,
    val trace: CalculationTrace
)
