package com.marcoacuna.payroll.domain.model

import com.marcoacuna.payroll.domain.error.PayrollError
import com.marcoacuna.payroll.domain.DomainResult
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Value object representing a gross monthly salary.
 * Accepts zero (months without accrual) but rejects negative values.
 * RE4 in the PAP rejects negative, not zero.
 */
@JvmInline
value class GrossSalary private constructor(val amount: BigDecimal) {

    companion object {
        fun of(amount: BigDecimal): DomainResult<GrossSalary> {
            if (amount < BigDecimal.ZERO) {
                return DomainResult.Failure(
                    PayrollError.InvalidGrossSalary("Gross salary cannot be negative: $amount")
                )
            }
            return DomainResult.Success(GrossSalary(amount.setScale(2, RoundingMode.HALF_UP)))
        }
    }

    /** Convert to PAP RE4 format: euro cents as BigDecimal (integer value, no decimals). */
    fun toRe4Cents(): BigDecimal = amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.DOWN)
}
