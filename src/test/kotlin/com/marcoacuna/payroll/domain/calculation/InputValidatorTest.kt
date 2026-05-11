package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.domain.model.*
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertIs

class InputValidatorTest {

    private fun baseInput() = EmployeeInput(
        grossMonthlySalary = GrossSalary.of(BigDecimal("4000.00")).getOrThrow(),
        stkl = TaxClass.I,
        kvz = BigDecimal("2.50"),
        federalState = FederalState.BAYERN,
        birthDate = LocalDate.of(1990, 5, 15),
        calculationDate = LocalDate.of(2025, 6, 15)
    )

    @Test
    fun `should accept valid basic input`() {
        val result = InputValidator.validate(baseInput())
        assertIs<DomainResult.Success<EmployeeInput>>(result)
    }

    @Test
    fun `should reject AF=true when STKL is not IV`() {
        val input = baseInput().copy(stkl = TaxClass.I, af = true, f = BigDecimal("0.900"))
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should accept AF=true with STKL IV and valid factor`() {
        val input = baseInput().copy(stkl = TaxClass.IV, af = true, f = BigDecimal("0.900"))
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Success<EmployeeInput>>(result)
    }

    @Test
    fun `should reject missing factor when AF=true`() {
        val input = baseInput().copy(stkl = TaxClass.IV, af = true, f = null)
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should reject factor present when AF=false`() {
        val input = baseInput().copy(af = false, f = BigDecimal("0.900"))
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should reject factor out of range`() {
        val input = baseInput().copy(stkl = TaxClass.IV, af = true, f = BigDecimal("1.500"))
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should reject ZKF in STKL V`() {
        val input = baseInput().copy(stkl = TaxClass.V, zkf = BigDecimal("1.0"))
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should reject ZKF in STKL VI`() {
        val input = baseInput().copy(stkl = TaxClass.VI, zkf = BigDecimal("0.5"))
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should reject LZZFREIB with AF=true`() {
        val input = baseInput().copy(
            stkl = TaxClass.IV, af = true, f = BigDecimal("0.900"),
            lzzfreib = BigDecimal("100")
        )
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should reject PVZ=true when employee is under 23`() {
        val input = baseInput().copy(
            pvz = true,
            birthDate = LocalDate.of(2005, 1, 1),
            calculationDate = LocalDate.of(2025, 6, 15)
        )
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should accept PVZ=true when employee is 23 or older`() {
        val input = baseInput().copy(
            pvz = true,
            birthDate = LocalDate.of(2000, 1, 1),
            calculationDate = LocalDate.of(2025, 6, 15)
        )
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Success<EmployeeInput>>(result)
    }

    @Test
    fun `should reject LZZFREIB in STKL VI`() {
        val input = baseInput().copy(stkl = TaxClass.VI, lzzfreib = BigDecimal("100"))
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should reject negative KVZ`() {
        val input = baseInput().copy(kvz = BigDecimal("-1.00"))
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `should reject invalid KRV`() {
        val input = baseInput().copy(krv = 5)
        val result = InputValidator.validate(input)
        assertIs<DomainResult.Failure>(result)
    }
}
