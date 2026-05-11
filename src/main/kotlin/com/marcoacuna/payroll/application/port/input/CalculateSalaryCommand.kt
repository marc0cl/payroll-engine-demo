package com.marcoacuna.payroll.application.port.input

import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.domain.error.PayrollError
import com.marcoacuna.payroll.domain.model.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Command to calculate net salary. Converts raw input into validated domain objects.
 */
data class CalculateSalaryCommand(
    val grossMonthlySalary: BigDecimal,
    val stkl: Int,
    val af: Boolean = false,
    val f: BigDecimal? = null,
    val zkf: BigDecimal = BigDecimal.ZERO,
    val lzzfreib: BigDecimal = BigDecimal.ZERO,
    val lzzhinzu: BigDecimal = BigDecimal.ZERO,
    val r: Int = 0,
    val kvz: BigDecimal,
    val krv: Int = 0,
    val pkv: Int = 0,
    val pkpv: BigDecimal = BigDecimal.ZERO,
    val pvs: Boolean = false,
    val pvz: Boolean = false,
    val federalState: String,
    val numberOfChildrenUnder25: Int = 0,
    val birthDate: LocalDate,
    val calculationDate: LocalDate
) {
    fun toEmployeeInput(): DomainResult<EmployeeInput> {
        val grossResult = GrossSalary.of(grossMonthlySalary)
        if (grossResult is DomainResult.Failure) return grossResult as DomainResult<EmployeeInput>
        val gross = (grossResult as DomainResult.Success).value

        val taxClass = TaxClass.fromInt(stkl)
            ?: return DomainResult.Failure(PayrollError.InvalidInput("stkl", "Invalid Steuerklasse: $stkl. Must be 1-6."))

        val state = try {
            FederalState.valueOf(federalState)
        } catch (e: IllegalArgumentException) {
            return DomainResult.Failure(
                PayrollError.InvalidInput("federalState", "Unknown federal state: $federalState")
            )
        }

        return DomainResult.Success(
            EmployeeInput(
                grossMonthlySalary = gross,
                stkl = taxClass,
                af = af,
                f = f,
                zkf = zkf,
                lzzfreib = lzzfreib,
                lzzhinzu = lzzhinzu,
                r = r,
                kvz = kvz,
                krv = krv,
                pkv = pkv,
                pkpv = pkpv,
                pvs = pvs,
                pvz = pvz,
                federalState = state,
                numberOfChildrenUnder25 = numberOfChildrenUnder25,
                birthDate = birthDate,
                calculationDate = calculationDate
            )
        )
    }
}
