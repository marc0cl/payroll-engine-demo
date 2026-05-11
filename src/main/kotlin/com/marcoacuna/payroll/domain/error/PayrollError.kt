package com.marcoacuna.payroll.domain.error

import java.time.LocalDate

/** Domain error hierarchy. These represent expected validation/business failures, not bugs. */
sealed class PayrollError {
    data class InvalidGrossSalary(val reason: String) : PayrollError()

    data class UnsupportedCalculationPeriod(
        val requestedDate: LocalDate,
        val availablePeriods: List<String>
    ) : PayrollError()

    data class InvalidInput(val field: String, val reason: String) : PayrollError()

    data class IncompatibleParameters(val details: String) : PayrollError()

    data class CalculationOverflow(val detail: String) : PayrollError()
}
