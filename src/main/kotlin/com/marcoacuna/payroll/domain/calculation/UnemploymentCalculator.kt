package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.audit.CalculationStep
import com.marcoacuna.payroll.domain.model.Deduction
import com.marcoacuna.payroll.domain.model.DeductionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Arbeitslosenversicherung (unemployment insurance) calculator.
 * Employee contribution = min(bpiSV, BBG) * employee rate.
 * KRV=2: not subject to statutory social insurance, exempt from AV.
 * §341 SGB III.
 */
class UnemploymentCalculator : DeductionCalculator {

    override fun calculate(context: CalculationContext) {
        val rates = context.rates.socialInsurance
        val krv = context.input.krv

        val base: BigDecimal
        val rate: BigDecimal
        val amount: BigDecimal
        val notes: String?

        if (krv != 2) {
            base = context.bpiSV.min(rates.unemploymentCeilingMonthly)
            if (base < context.bpiSV) {
                context.capsApplied.add("BBG_AV=${rates.unemploymentCeilingMonthly}")
            }
            rate = rates.unemploymentEmployeeRate
            amount = base.multiply(rate).setScale(2, RoundingMode.HALF_UP)
            notes = if (context.isUebergangsbereich) "Uebergangsbereich: reduced BE applied" else null
        } else {
            base = BigDecimal.ZERO
            rate = BigDecimal.ZERO
            amount = BigDecimal.ZERO
            notes = "KRV=2: not subject to statutory social insurance"
        }

        context.unemploymentContribution = amount
        context.deductions.add(
            Deduction(DeductionType.ARBEITSLOSENVERSICHERUNG, base, rate, amount, "§341 SGB III")
        )
        context.steps.add(
            CalculationStep(
                DeductionType.ARBEITSLOSENVERSICHERUNG, base, rate, amount,
                "§341 SGB III", "HALF_UP scale 2", notes
            )
        )
    }
}
