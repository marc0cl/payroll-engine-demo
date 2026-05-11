package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.model.EmployeeInput
import com.marcoacuna.payroll.domain.model.TaxClass
import java.math.BigDecimal

/**
 * Maps EmployeeInput fields to PAP variable names.
 * This is a data transfer object aligning our domain model with the official
 * BMF Programmablaufplan variable nomenclature.
 */
data class PapInput(
    /** RE4: Brutto-Arbeitslohn in Cent fuer den Lohnzahlungszeitraum. */
    val re4: BigDecimal,
    /** LZZ: Lohnzahlungszeitraum. Always 2 (monthly) in this engine. */
    val lzz: Int = 2,
    /** STKL: Steuerklasse 1-6. */
    val stkl: Int,
    /** AF: 1 if Faktorverfahren active, 0 otherwise. */
    val af: Int,
    /** F: Faktor with 3 decimal places. 1.000 if AF=0. */
    val f: BigDecimal,
    /** ZKF: Zahl der Kinderfreibetraege. */
    val zkf: BigDecimal,
    /** LZZFREIB: Freibetrag fuer den LZZ in Cent. */
    val lzzfreib: BigDecimal,
    /** LZZHINZU: Hinzurechnungsbetrag fuer den LZZ in Cent. */
    val lzzhinzu: BigDecimal,
    /** R: Religionszugehoerigkeit. 0=no, 1=yes. */
    val r: Int,
    /** KVZ: Zusatzbeitragssatz in percent (e.g., 2.90). */
    val kvz: BigDecimal,
    /** KRV: 0=RV-pflichtig, 1=befreit, 2=nicht in gesetzlicher RV. */
    val krv: Int,
    /** PKV: 0=gesetzlich, 1=privat ohne AG-Zuschuss, 2=privat mit AG-Zuschuss. */
    val pkv: Int,
    /** PKPV: Jaehrlicher PKV-Beitrag in Cent. */
    val pkpv: BigDecimal,
    /** PVS: 1 if Sachsen, 0 otherwise. */
    val pvs: Int,
    /** PVZ: 1 if childless and >= 23, 0 otherwise. */
    val pvz: Int
)

/** Maps from domain EmployeeInput to PAP input variables. */
object PapInputMapper {

    fun map(input: EmployeeInput): PapInput = PapInput(
        re4 = input.grossMonthlySalary.toRe4Cents(),
        lzz = 2,
        stkl = input.stkl.value,
        af = if (input.af) 1 else 0,
        f = input.f ?: BigDecimal("1.000"),
        zkf = input.zkf,
        lzzfreib = input.lzzfreib,
        lzzhinzu = input.lzzhinzu,
        r = input.r,
        kvz = input.kvz,
        krv = input.krv,
        pkv = input.pkv,
        pkpv = input.pkpv.multiply(BigDecimal(12)),  // monthly → annual for PAP
        pvs = if (input.pvs) 1 else 0,
        pvz = if (input.pvz) 1 else 0
    )
}
