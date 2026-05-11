package com.marcoacuna.payroll.domain.model

import java.math.BigDecimal

/**
 * A single deduction line in the payroll calculation.
 * Each deduction records its type, the base on which it was calculated,
 * the applied rate, the resulting amount, and the legal reference.
 */
data class Deduction(
    val type: DeductionType,
    val calculationBase: BigDecimal,
    val rate: BigDecimal,
    val amount: BigDecimal,
    val legalReference: String
)
