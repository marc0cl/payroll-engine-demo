package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.audit.CalculationStep
import com.marcoacuna.payroll.domain.model.Deduction
import com.marcoacuna.payroll.domain.model.DeductionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Rentenversicherung (pension insurance) calculator.
 * Employee contribution = min(bpiSV, BBG) * employee rate.
 * KRV=0: subject to pension insurance.
 * KRV=1 or KRV=2: exempt, contribution = 0.
 * §158 SGB VI.
 */
class PensionCalculator : DeductionCalculator {

    override fun calculate(context: CalculationContext) {
        val rates = context.rates.socialInsurance
        val krv = context.input.krv

        val base: BigDecimal
        val rate: BigDecimal
        val amount: BigDecimal
        val notes: String?

        if (krv == 0) {
            base = context.bpiSV.min(rates.pensionCeilingMonthly)
            if (base < context.bpiSV) {
                context.capsApplied.add("BBG_RV=${rates.pensionCeilingMonthly}")
            }
            rate = rates.pensionEmployeeRate
            amount = base.multiply(rate).setScale(2, RoundingMode.HALF_UP)
            notes = if (context.isUebergangsbereich) "Uebergangsbereich: reduced BE applied" else null
        } else {
            base = BigDecimal.ZERO
            rate = BigDecimal.ZERO
            amount = BigDecimal.ZERO
            notes = "KRV=$krv: exempt from Rentenversicherung"
        }

        context.pensionContribution = amount
        context.deductions.add(
            Deduction(DeductionType.RENTENVERSICHERUNG, base, rate, amount, "§158 SGB VI")
        )
        context.steps.add(
            CalculationStep(
                DeductionType.RENTENVERSICHERUNG, base, rate, amount,
                "§158 SGB VI", "HALF_UP scale 2", notes
            )
        )
    }
}
