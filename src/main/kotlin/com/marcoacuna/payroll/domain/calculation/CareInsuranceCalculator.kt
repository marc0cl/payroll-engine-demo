package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.audit.CalculationStep
import com.marcoacuna.payroll.domain.model.Deduction
import com.marcoacuna.payroll.domain.model.DeductionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pflegeversicherung (care insurance) calculator.
 * §55 Abs. 3 SGB XI as amended by PUEG.
 *
 * Employee rate depends on number of children under 25:
 * - 0 children, age >= 23 (PVZ=true): base + childless surcharge (e.g., 1.8% + 0.6% = 2.4%)
 * - 0 children, age < 23 (PVZ=false): base (1.8%)
 * - 1 child: base (1.8%)
 * - 2 children under 25: base - 0.25% = 1.55%
 * - 3 children under 25: base - 0.50% = 1.30%
 * - 4 children under 25: base - 0.75% = 1.05%
 * - 5+ children under 25: base - 1.00% = 0.80% (capped)
 *
 * Sachsen: employee pays extra 0.5%, employer pays 0.5% less (Buss- und Bettag compensation).
 */
class CareInsuranceCalculator : DeductionCalculator {

    override fun calculate(context: CalculationContext) {
        val pvRates = context.rates.pflegeversicherung
        val input = context.input

        val base = context.bpiSV.min(context.rates.socialInsurance.healthInsuranceCeilingMonthly)
        if (base < context.bpiSV && !context.capsApplied.any { it.startsWith("BBG_KV") }) {
            context.capsApplied.add("BBG_KV=${context.rates.socialInsurance.healthInsuranceCeilingMonthly}")
        }

        var employeeRate = pvRates.employeeBaseShare

        // Childless surcharge
        if (input.pvz) {
            employeeRate = employeeRate.add(pvRates.childlessSurcharge)
        }

        // Child-based reduction (2nd to 5th child under 25)
        val childrenUnder25 = input.numberOfChildrenUnder25
        if (childrenUnder25 >= 2) {
            val reductionChildren = (childrenUnder25 - 1)
                .coerceAtMost(pvRates.maxChildrenForReduction - 1)
            val reduction = pvRates.childReductionPerChild
                .multiply(BigDecimal(reductionChildren))
            employeeRate = employeeRate.subtract(reduction)
        }

        // Sachsen: employee pays extra
        if (input.pvs) {
            employeeRate = employeeRate.add(pvRates.sachsenEmployeeExtra)
        }

        // Floor at zero
        employeeRate = employeeRate.max(BigDecimal.ZERO)

        val amount = base.multiply(employeeRate).setScale(2, RoundingMode.HALF_UP)

        val notes = buildString {
            append("PVZ=${input.pvz}")
            append(", children_u25=${childrenUnder25}")
            if (input.pvs) append(", Sachsen(+${pvRates.sachsenEmployeeExtra})")
            append(", effective_rate=$employeeRate")
        }

        context.careInsuranceContribution = amount
        context.deductions.add(
            Deduction(DeductionType.PFLEGEVERSICHERUNG, base, employeeRate, amount, "§55 Abs. 3 SGB XI")
        )
        context.steps.add(
            CalculationStep(
                DeductionType.PFLEGEVERSICHERUNG, base, employeeRate, amount,
                "§55 Abs. 3 SGB XI", "HALF_UP scale 2", notes
            )
        )
    }
}
