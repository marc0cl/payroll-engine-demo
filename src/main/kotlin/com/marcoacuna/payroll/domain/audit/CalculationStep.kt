package com.marcoacuna.payroll.domain.audit

import com.marcoacuna.payroll.domain.model.DeductionType
import java.math.BigDecimal

/**
 * A single step in the calculation trace, providing auditability.
 * Each step records what was calculated, on what base, using which rate,
 * the legal reference, and how rounding was applied.
 */
data class CalculationStep(
    val deductionType: DeductionType,
    val calculationBase: BigDecimal,
    val appliedRate: BigDecimal,
    val amount: BigDecimal,
    val legalReference: String,
    val roundingInfo: String,
    val notes: String? = null
)
