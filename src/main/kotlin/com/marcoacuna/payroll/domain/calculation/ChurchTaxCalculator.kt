package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.audit.CalculationStep
import com.marcoacuna.payroll.domain.model.Deduction
import com.marcoacuna.payroll.domain.model.DeductionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Kirchensteuer (church tax) calculator.
 * Applied only if R > 0 (employee is church tax liable).
 * Amount = BK (Bemessungsgrundlage from PAP) * Kirchensteuer rate of the federal state.
 * Rate: 8% in Baden-Wuerttemberg and Bayern, 9% in all other states.
 */
class ChurchTaxCalculator : DeductionCalculator {

    override fun calculate(context: CalculationContext) {
        val r = context.input.r
        val federalState = context.input.federalState

        val base: BigDecimal
        val rate: BigDecimal
        val amount: BigDecimal
        val notes: String?

        if (r > 0 && context.kirchensteuerBasis > BigDecimal.ZERO) {
            base = context.kirchensteuerBasis
            rate = federalState.kirchensteuerRate
            amount = base.multiply(rate).setScale(2, RoundingMode.HALF_UP)
            notes = "R=$r, state=${federalState.name}, rate=${federalState.kirchensteuerRate}"
        } else {
            base = BigDecimal.ZERO
            rate = BigDecimal.ZERO
            amount = BigDecimal.ZERO
            notes = if (r == 0) "R=0: not church tax liable" else "KiSt basis is zero"
        }

        context.deductions.add(
            Deduction(DeductionType.KIRCHENSTEUER, base, rate, amount, "KiStG ${federalState.name}")
        )
        context.steps.add(
            CalculationStep(
                DeductionType.KIRCHENSTEUER, base, rate, amount,
                "KiStG ${federalState.name}", "HALF_UP scale 2", notes
            )
        )
    }
}
