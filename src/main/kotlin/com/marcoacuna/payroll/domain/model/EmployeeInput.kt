package com.marcoacuna.payroll.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Input for a payroll calculation. Variables are isomorphic to the official
 * BMF PAP contract where applicable, plus social insurance parameters.
 *
 * PAP variable naming conventions are preserved in field names and documented
 * in comments for traceability against the official specification.
 */
data class EmployeeInput(
    val grossMonthlySalary: GrossSalary,

    // --- PAP input variables (§39b EStG) ---

    /** STKL: Steuerklasse (tax class) 1-6 */
    val stkl: TaxClass,

    /** AF: 1 = Faktorverfahren active. Only valid for STKL IV. */
    val af: Boolean = false,

    /** F: Factor for Faktorverfahren, 3 decimal places, range (0.000, 1.000].
     *  Required if af=true, must be null if af=false. */
    val f: BigDecimal? = null,

    /** ZKF: Zahl der Kinderfreibetraege (child allowances).
     *  Can be 0.5 increments (split custody). Only valid in STKL I-IV, must be 0 in V/VI. */
    val zkf: BigDecimal = BigDecimal.ZERO,

    /** LZZFREIB: Freibetrag for the Lohnzahlungszeitraum (monthly), in euro cents.
     *  Must be 0 if af=true (Freibetrag and Faktor are mutually exclusive). */
    val lzzfreib: BigDecimal = BigDecimal.ZERO,

    /** LZZHINZU: Hinzurechnungsbetrag for the Lohnzahlungszeitraum (monthly), in euro cents. */
    val lzzhinzu: BigDecimal = BigDecimal.ZERO,

    /** R: Religion code. 0 = no church tax, 1 = church tax liable (ev/rk). */
    val r: Int = 0,

    /** KVZ: Zusatzbeitragssatz of the employee's Krankenkasse.
     *  Full percentage value, e.g. 2.90 for 2.90%. NOT a fraction like 0.029. */
    val kvz: BigDecimal,

    /** KRV: Rentenversicherung status.
     *  0 = RV-pflichtig (subject to pension insurance),
     *  1 = RV-befreit (exempt from RV but not from other SV),
     *  2 = nicht in gesetzlicher RV (not subject to statutory pension at all).
     *  Note: Since 2025, BBG is national (no West/East distinction). */
    val krv: Int = 0,

    /** PKV: Private Krankenversicherung status.
     *  0 = gesetzlich versichert (statutory health insurance),
     *  1 = privat versichert ohne AG-Zuschuss,
     *  2 = privat versichert mit AG-Zuschuss. */
    val pkv: Int = 0,

    /** PKPV: Monthly PKV contribution in euro cents.
     *  Only relevant if pkv > 0. */
    val pkpv: BigDecimal = BigDecimal.ZERO,

    /** PVS: true if the employee works in Sachsen.
     *  Sachsen has a special PV split: employee pays extra 0.5%, employer pays 0.5% less. */
    val pvs: Boolean = false,

    /** PVZ: true if the employee is childless AND >= 23 years old.
     *  Triggers the Kinderlosenzuschlag of 0.6% on Pflegeversicherung. */
    val pvz: Boolean = false,

    // --- Social insurance parameters (outside PAP scope) ---

    /** Federal state — determines Kirchensteuer rate and Sachsen PV rule. */
    val federalState: FederalState,

    /** Number of children under 25 for PV Abschlag calculation (§55 Abs. 3 SGB XI).
     *  Each child from 2nd to 5th reduces employee PV rate by 0.25%. */
    val numberOfChildrenUnder25: Int = 0,

    /** Employee birth date — used to verify PVZ age threshold (>= 23). */
    val birthDate: LocalDate,

    // --- Calculation period ---

    /** Determines which PAP version and which effective rates apply. */
    val calculationDate: LocalDate
)
