package com.marcoacuna.payroll.domain.calculation

/** Interface for individual deduction calculators in the pipeline. */
interface DeductionCalculator {
    fun calculate(context: CalculationContext)
}
