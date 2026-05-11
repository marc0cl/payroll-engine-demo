package com.marcoacuna.payroll.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Value object representing the calculated net monthly salary. */
@JvmInline
value class NetSalary(val amount: BigDecimal) {
    companion object {
        fun of(amount: BigDecimal): NetSalary =
            NetSalary(amount.setScale(2, RoundingMode.HALF_UP))
    }
}
