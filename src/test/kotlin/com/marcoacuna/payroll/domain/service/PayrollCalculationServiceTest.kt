package com.marcoacuna.payroll.domain.service

import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.domain.calculation.testInput
import com.marcoacuna.payroll.domain.calculation.testRates
import com.marcoacuna.payroll.domain.model.*
import com.marcoacuna.payroll.domain.rates.EffectiveRates
import com.marcoacuna.payroll.domain.rates.RatesProvider
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PayrollCalculationServiceTest {

    private val rates = testRates()
    private val ratesProvider = object : RatesProvider {
        override fun ratesFor(date: LocalDate): EffectiveRates? {
            return if (!date.isBefore(rates.effectiveFrom) &&
                (rates.effectiveUntil == null || !date.isAfter(rates.effectiveUntil!!))
            ) rates else null
        }
        override fun allPeriods(): List<EffectiveRates> = listOf(rates)
    }
    private val service = PayrollCalculationService(ratesProvider)

    @Test
    fun `should calculate complete pipeline for STKL I`() {
        val input = testInput(gross = BigDecimal("4000.00"), stkl = TaxClass.I, pvz = true, r = 1)
        val result = service.calculate(input)
        assertIs<DomainResult.Success<PayrollResult>>(result)

        val payroll = result.value
        assertEquals(7, payroll.deductions.size)
        assertEquals(CalculationMode.OFFICIAL_PAP_LZZ2, payroll.calculationMode)

        // Net = gross - total deductions
        val expectedNet = payroll.grossSalary.amount.subtract(payroll.totalDeductions)
        assertEquals(expectedNet.setScale(2), payroll.netSalary.amount)

        // All deductions are non-negative
        payroll.deductions.forEach { d ->
            assertTrue(d.amount >= BigDecimal.ZERO, "${d.type} amount should be >= 0, was ${d.amount}")
        }
    }

    @Test
    fun `should return error for unsupported calculation period`() {
        val input = testInput(calculationDate = LocalDate.of(2020, 1, 1))
        val result = service.calculate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should be deterministic for same input`() {
        val input = testInput(gross = BigDecimal("5000.00"), stkl = TaxClass.III)
        val r1 = service.calculate(input) as DomainResult.Success
        val r2 = service.calculate(input) as DomainResult.Success
        assertEquals(r1.value.totalDeductions, r2.value.totalDeductions)
        assertEquals(r1.value.netSalary, r2.value.netSalary)
    }

    @Test
    fun `should handle zero salary`() {
        val input = testInput(gross = BigDecimal("0.00"))
        val result = service.calculate(input)
        assertIs<DomainResult.Success<PayrollResult>>(result)
        assertEquals(BigDecimal("0.00"), result.value.totalDeductions.setScale(2))
        assertEquals(BigDecimal("0.00"), result.value.netSalary.amount)
    }

    @Test
    fun `should include trace with all steps and legal references`() {
        val input = testInput(gross = BigDecimal("4000.00"), r = 1, pvz = true)
        val result = service.calculate(input) as DomainResult.Success
        val trace = result.value.trace

        assertEquals(CalculationMode.OFFICIAL_PAP_LZZ2, trace.calculationMode)
        assertEquals("PAP2025_Maerz", trace.papVersion)
        assertTrue(trace.steps.size >= 7, "Expected at least 7 trace steps, got ${trace.steps.size}")
        trace.steps.forEach { step ->
            assertTrue(step.legalReference.isNotBlank(), "Legal reference should not be blank for ${step.deductionType}")
        }
    }

    @Test
    fun `should reject invalid input before calculation`() {
        val input = testInput(stkl = TaxClass.I).copy(af = true, f = BigDecimal("0.900"))
        val result = service.calculate(input)
        assertIs<DomainResult.Failure>(result)
    }
}
