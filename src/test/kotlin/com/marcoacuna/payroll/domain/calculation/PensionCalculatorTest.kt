package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.model.*
import com.marcoacuna.payroll.domain.rates.*
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PensionCalculatorTest {

    private val calculator = PensionCalculator()
    private val rates2025 = testRates()

    private fun context(gross: BigDecimal, krv: Int = 0): CalculationContext {
        val input = testInput(gross, krv = krv)
        return CalculationContext(input = input, rates = rates2025)
    }

    @Test
    fun `should calculate pension at standard rate when salary below ceiling`() {
        val ctx = context(BigDecimal("4000.00"))
        calculator.calculate(ctx)
        // 4000 * 0.093 = 372.00
        assertEquals(BigDecimal("372.00"), ctx.pensionContribution)
    }

    @Test
    fun `should cap at BBG when salary exceeds ceiling`() {
        val ctx = context(BigDecimal("10000.00"))
        calculator.calculate(ctx)
        // min(10000, 8050) * 0.093 = 8050 * 0.093 = 748.65
        assertEquals(BigDecimal("748.65"), ctx.pensionContribution)
    }

    @Test
    fun `should calculate exactly at BBG boundary`() {
        val ctx = context(BigDecimal("8050.00"))
        calculator.calculate(ctx)
        assertEquals(BigDecimal("748.65"), ctx.pensionContribution)
    }

    @Test
    fun `should return zero when KRV=2 (exempt)`() {
        val ctx = context(BigDecimal("4000.00"), krv = 2)
        calculator.calculate(ctx)
        assertEquals(BigDecimal.ZERO, ctx.pensionContribution)
    }

    @Test
    fun `should return zero for zero salary`() {
        val ctx = context(BigDecimal("0.00"))
        calculator.calculate(ctx)
        assertEquals(BigDecimal("0.00"), ctx.pensionContribution)
    }

    @Test
    fun `should add deduction and step to context`() {
        val ctx = context(BigDecimal("4000.00"))
        calculator.calculate(ctx)
        assertEquals(1, ctx.deductions.size)
        assertEquals(DeductionType.RENTENVERSICHERUNG, ctx.deductions[0].type)
        assertEquals(1, ctx.steps.size)
        assertTrue(ctx.steps[0].legalReference.contains("§158 SGB VI"))
    }
}
