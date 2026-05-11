package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.audit.CalculationStep
import com.marcoacuna.payroll.domain.model.Deduction
import com.marcoacuna.payroll.domain.model.DeductionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Krankenversicherung (health insurance) calculator.
 * For GKV (pkv=0): employee share = min(bpiSV, BBG) * (general employee rate + KVZ/2).
 * For PKV (pkv>0): GKV contribution is 0 (employee pays PKPV separately, outside this calc).
 * §223 SGB V.
 *
 * KVZ is the full Zusatzbeitragssatz percentage (e.g. 2.90 for 2.90%),
 * employee pays half of it.
 */
class HealthInsuranceCalculator : DeductionCalculator {

    override fun calculate(context: CalculationContext) {
        val rates = context.rates.socialInsurance
        val pkv = context.input.pkv
        val kvz = context.input.kvz

        val base: BigDecimal
        val rate: BigDecimal
        val amount: BigDecimal
        val notes: String?

        if (pkv == 0) {
            base = context.bpiSV.min(rates.healthInsuranceCeilingMonthly)
            if (base < context.bpiSV) {
                context.capsApplied.add("BBG_KV=${rates.healthInsuranceCeilingMonthly}")
            }
            // Employee rate = general employee share + half of Zusatzbeitrag
            // KVZ is in percentage points (e.g. 2.90), convert to fraction: /100, then /2
            val kvzEmployeeShare = kvz.divide(BigDecimal(200), 10, RoundingMode.HALF_UP)
            rate = rates.healthInsuranceEmployeeRate.add(kvzEmployeeShare)
            amount = base.multiply(rate).setScale(2, RoundingMode.HALF_UP)
            notes = "GKV: general ${rates.healthInsuranceEmployeeRate} + KVZ/2 $kvzEmployeeShare"
        } else {
            base = BigDecimal.ZERO
            rate = BigDecimal.ZERO
            amount = BigDecimal.ZERO
            notes = "PKV=$pkv: employee pays private insurance (PKPV) separately"
        }

        context.healthInsuranceContribution = amount
        context.deductions.add(
            Deduction(DeductionType.KRANKENVERSICHERUNG, base, rate, amount, "§223 SGB V")
        )
        context.steps.add(
            CalculationStep(
                DeductionType.KRANKENVERSICHERUNG, base, rate, amount,
                "§223 SGB V", "HALF_UP scale 2", notes
            )
        )
    }
}
