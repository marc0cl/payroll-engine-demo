package com.marcoacuna.payroll.domain.calculation

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class CareInsuranceCalculatorTest {

    private val calculator = CareInsuranceCalculator()
    private val rates = testRates()

    private fun context(
        gross: BigDecimal = BigDecimal("4000.00"),
        pvz: Boolean = false,
        childrenUnder25: Int = 0,
        pvs: Boolean = false
    ): CalculationContext {
        val input = testInput(gross, pvz = pvz, numberOfChildrenUnder25 = childrenUnder25, pvs = pvs)
        return CalculationContext(input = input, rates = rates)
    }

    @Test
    fun `should apply childless surcharge when PVZ=true`() {
        val ctx = context(pvz = true)
        calculator.calculate(ctx)
        // 4000 * (0.018 + 0.006) = 4000 * 0.024 = 96.00
        assertEquals(BigDecimal("96.00"), ctx.careInsuranceContribution)
    }

    @Test
    fun `should use base rate when PVZ=false and 0 children`() {
        val ctx = context(pvz = false, childrenUnder25 = 0)
        calculator.calculate(ctx)
        // 4000 * 0.018 = 72.00
        assertEquals(BigDecimal("72.00"), ctx.careInsuranceContribution)
    }

    @Test
    fun `should use base rate for 1 child`() {
        val ctx = context(childrenUnder25 = 1)
        calculator.calculate(ctx)
        // 4000 * 0.018 = 72.00
        assertEquals(BigDecimal("72.00"), ctx.careInsuranceContribution)
    }

    @Test
    fun `should reduce by 0_25 percent for 2 children under 25`() {
        val ctx = context(childrenUnder25 = 2)
        calculator.calculate(ctx)
        // 4000 * (0.018 - 0.0025) = 4000 * 0.0155 = 62.00
        assertEquals(BigDecimal("62.00"), ctx.careInsuranceContribution)
    }

    @Test
    fun `should reduce by 0_50 percent for 3 children under 25`() {
        val ctx = context(childrenUnder25 = 3)
        calculator.calculate(ctx)
        // 4000 * (0.018 - 0.0050) = 4000 * 0.013 = 52.00
        assertEquals(BigDecimal("52.00"), ctx.careInsuranceContribution)
    }

    @Test
    fun `should cap reduction at 5 children`() {
        val ctx = context(childrenUnder25 = 6)
        calculator.calculate(ctx)
        // max reduction = 4 * 0.0025 = 0.01. Rate = 0.018 - 0.01 = 0.008
        // 4000 * 0.008 = 32.00
        assertEquals(BigDecimal("32.00"), ctx.careInsuranceContribution)
    }

    @Test
    fun `should add Sachsen extra`() {
        val ctx = context(pvs = true, childrenUnder25 = 1)
        calculator.calculate(ctx)
        // 4000 * (0.018 + 0.005) = 4000 * 0.023 = 92.00
        assertEquals(BigDecimal("92.00"), ctx.careInsuranceContribution)
    }

    @Test
    fun `should apply Sachsen extra with childless surcharge`() {
        val ctx = context(pvz = true, pvs = true)
        calculator.calculate(ctx)
        // 4000 * (0.018 + 0.006 + 0.005) = 4000 * 0.029 = 116.00
        assertEquals(BigDecimal("116.00"), ctx.careInsuranceContribution)
    }
}
