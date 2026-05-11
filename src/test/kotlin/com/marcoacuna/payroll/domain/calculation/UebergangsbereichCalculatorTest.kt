package com.marcoacuna.payroll.domain.calculation

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UebergangsbereichCalculatorTest {

    private val calculator = UebergangsbereichCalculator()
    private val rates = testRates()

    private fun context(gross: BigDecimal): CalculationContext {
        val input = testInput(gross)
        return CalculationContext(input = input, rates = rates)
    }

    @Test
    fun `should flag Uebergangsbereich for salary just above lower threshold`() {
        val ctx = context(BigDecimal("556.01"))
        calculator.calculate(ctx)
        assertTrue(ctx.isUebergangsbereich)
        assertTrue(ctx.bpiSV < BigDecimal("556.01"))
    }

    @Test
    fun `should compute reduced BE in the middle of the zone`() {
        val ctx = context(BigDecimal("1200.00"))
        calculator.calculate(ctx)
        assertTrue(ctx.isUebergangsbereich)
        assertTrue(ctx.bpiSV > BigDecimal("556.00"))
        assertTrue(ctx.bpiSV < BigDecimal("1200.00"))
    }

    @Test
    fun `should converge to near-full salary near upper threshold`() {
        val ctx = context(BigDecimal("1999.99"))
        calculator.calculate(ctx)
        assertTrue(ctx.isUebergangsbereich)
        // BE should be very close to actual gross (converges as AE approaches T)
        assertTrue(ctx.bpiSV <= BigDecimal("1999.99"))
        assertTrue(ctx.bpiSV > BigDecimal("1900.00"), "BE should be close to gross near upper bound")
    }

    @Test
    fun `should not be Uebergangsbereich at exactly 2000`() {
        val ctx = context(BigDecimal("2000.00"))
        calculator.calculate(ctx)
        assertFalse(ctx.isUebergangsbereich)
        assertEquals(BigDecimal("2000.00"), ctx.bpiSV)
    }

    @Test
    fun `should not be Uebergangsbereich above 2000`() {
        val ctx = context(BigDecimal("4000.00"))
        calculator.calculate(ctx)
        assertFalse(ctx.isUebergangsbereich)
        assertEquals(BigDecimal("4000.00"), ctx.bpiSV)
    }

    @Test
    fun `should not be Uebergangsbereich at or below lower threshold`() {
        val ctx = context(BigDecimal("556.00"))
        calculator.calculate(ctx)
        assertFalse(ctx.isUebergangsbereich)
        assertEquals(BigDecimal("556.00"), ctx.bpiSV)
    }
}
