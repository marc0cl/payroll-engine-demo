package com.marcoacuna.payroll.infrastructure.adapter.input.rest.dto

import com.marcoacuna.payroll.application.port.input.CalculateSalaryCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CalculationRequest(
    @field:NotNull
    val grossMonthlySalary: BigDecimal,

    @field:NotNull
    @field:Min(1)
    val stkl: Int,

    val af: Boolean = false,
    val f: BigDecimal? = null,
    val zkf: BigDecimal = BigDecimal.ZERO,
    val lzzfreib: BigDecimal = BigDecimal.ZERO,
    val lzzhinzu: BigDecimal = BigDecimal.ZERO,
    val r: Int = 0,

    @field:NotNull
    val kvz: BigDecimal,

    val krv: Int = 0,
    val pkv: Int = 0,
    val pkpv: BigDecimal = BigDecimal.ZERO,
    val pvs: Boolean = false,
    val pvz: Boolean = false,

    @field:NotNull
    val federalState: String,

    val numberOfChildrenUnder25: Int = 0,

    @field:NotNull
    val birthDate: LocalDate,

    @field:NotNull
    val calculationDate: LocalDate
) {
    fun toCommand(): CalculateSalaryCommand = CalculateSalaryCommand(
        grossMonthlySalary = grossMonthlySalary,
        stkl = stkl,
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
        federalState = federalState,
        numberOfChildrenUnder25 = numberOfChildrenUnder25,
        birthDate = birthDate,
        calculationDate = calculationDate
    )
}
