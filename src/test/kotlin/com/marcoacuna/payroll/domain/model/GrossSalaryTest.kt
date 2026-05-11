package com.marcoacuna.payroll.domain.model

import com.marcoacuna.payroll.domain.DomainResult
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GrossSalaryTest {

    @Test
    fun `should reject negative salary`() {
        val result = GrossSalary.of(BigDecimal("-1.00"))
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should accept zero salary for months without accrual`() {
        val result = GrossSalary.of(BigDecimal("0.00"))
        assertIs<DomainResult.Success<GrossSalary>>(result)
        assertEquals(BigDecimal("0.00"), result.value.amount)
    }

    @Test
    fun `should accept positive salary`() {
        val result = GrossSalary.of(BigDecimal("4500.00"))
        assertIs<DomainResult.Success<GrossSalary>>(result)
        assertEquals(BigDecimal("4500.00"), result.value.amount)
    }

    @Test
    fun `should normalize scale to 2`() {
        val result = GrossSalary.of(BigDecimal("3000.5"))
        assertIs<DomainResult.Success<GrossSalary>>(result)
        assertEquals(2, result.value.amount.scale())
        assertEquals(BigDecimal("3000.50"), result.value.amount)
    }

    @Test
    fun `should convert to RE4 cents correctly`() {
        val result = GrossSalary.of(BigDecimal("4500.75"))
        assertIs<DomainResult.Success<GrossSalary>>(result)
        assertEquals(BigDecimal("450075"), result.value.toRe4Cents())
    }
}
