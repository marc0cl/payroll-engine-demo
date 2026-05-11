package com.marcoacuna.payroll.domain.calculation

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Determines if the employee is in the Uebergangsbereich (Midijob zone)
 * and computes the reduced beitragspflichtige Einnahme (BE) per §20 Abs. 2a SGB IV.
 *
 * Zone: Geringfuegigkeitsgrenze < gross <= 2000 EUR
 *
 * Formula:
 *   BE = F * G + ((T / (T - G)) - (G / (T - G)) * F) * (AE - G)
 * where G = lower threshold, T = upper threshold, AE = actual gross, F = factor.
 *
 * Below G: Minijob (out of scope for this engine).
 * Above T: Regular employment, BE = gross.
 */
class UebergangsbereichCalculator : DeductionCalculator {

    override fun calculate(context: CalculationContext) {
        val gross = context.input.grossMonthlySalary.amount
        val params = context.rates.uebergangsbereich
        val g = params.lowerThreshold
        val t = params.upperThreshold
        val f = params.factor

        if (gross > g && gross < t) {
            context.isUebergangsbereich = true
            val mc = MathContext(20)
            val tMinusG = t.subtract(g, mc)
            val tDivTMinusG = t.divide(tMinusG, 20, RoundingMode.HALF_UP)
            val gDivTMinusG = g.divide(tMinusG, 20, RoundingMode.HALF_UP)

            // BE = F * G + ((T / (T - G)) - (G / (T - G)) * F) * (AE - G)
            val part1 = f.multiply(g, mc)
            val part2coeff = tDivTMinusG.subtract(gDivTMinusG.multiply(f, mc), mc)
            val part2 = part2coeff.multiply(gross.subtract(g, mc), mc)
            val be = part1.add(part2, mc).setScale(2, RoundingMode.HALF_UP)

            context.bpiSV = be
        } else {
            // At or above T: normal employment. At or below G: Minijob (not handled).
            context.isUebergangsbereich = false
            context.bpiSV = gross
        }
    }
}
