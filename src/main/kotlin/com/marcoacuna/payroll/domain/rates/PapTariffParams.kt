package com.marcoacuna.payroll.domain.rates

import java.math.BigDecimal

/**
 * Year-specific parameters for the §32a EStG income tax tariff calculation.
 *
 * These values are published by the BMF in each PAP version and encode the
 * progressive tax formula's zone boundaries, polynomial coefficients,
 * and related allowances. They MUST match the official PAP XML for the
 * corresponding [EffectiveRates.papVersion].
 *
 * The tariff formula (§32a Abs. 1 EStG) has five zones:
 * - Zone 1: 0..grundfreibetrag → 0 EUR tax
 * - Zone 2: grundfreibetrag+1..zone2Upper → (zone2A * y + zone2B) * y
 * - Zone 3: zone2Upper+1..zone3Upper → (zone3A * z + zone3B) * z + zone3C
 * - Zone 4: zone3Upper+1..zone4Upper → zone4Rate * zvE - zone4Sub
 * - Zone 5: above zone4Upper → zone5Rate * zvE - zone5Sub
 */
data class PapTariffParams(

    // --- §32a EStG tariff zone boundaries (annual EUR) ---

    /** Grundfreibetrag: tax-free basic allowance. */
    val grundfreibetrag: BigDecimal,
    /** Upper bound of zone 2 (progressive 14%→~24%). */
    val zone2Upper: BigDecimal,
    /** Upper bound of zone 3 (progressive ~24%→42%). */
    val zone3Upper: BigDecimal,
    /** Upper bound of zone 4 (Spitzensteuersatz 42%). Above = Reichensteuer 45%. */
    val zone4Upper: BigDecimal,

    // --- §32a tariff polynomial coefficients ---

    /** Zone 2: first polynomial factor. */
    val zone2A: BigDecimal,
    /** Zone 2: second polynomial factor. */
    val zone2B: BigDecimal,
    /** Zone 3: first polynomial factor. */
    val zone3A: BigDecimal,
    /** Zone 3: second polynomial factor. */
    val zone3B: BigDecimal,
    /** Zone 3: constant offset. */
    val zone3C: BigDecimal,
    /** Zone 4: marginal rate (typically 0.42). */
    val zone4Rate: BigDecimal,
    /** Zone 4: subtraction constant for continuity. */
    val zone4Sub: BigDecimal,
    /** Zone 5: marginal rate (typically 0.45, Reichensteuer). */
    val zone5Rate: BigDecimal,
    /** Zone 5: subtraction constant for continuity. */
    val zone5Sub: BigDecimal,

    // --- §3 SolZG: Solidaritaetszuschlag Freigrenzen ---

    /** Soli exemption threshold (annual Lohnsteuer). */
    val soliFreigrenze: BigDecimal,
    /** Soli exemption threshold for Steuerklasse III (doubled). */
    val soliFreigrenzeSTKL3: BigDecimal,

    // --- §32 Abs. 6 EStG ---

    /** Kinderfreibetrag per child allowance count (ZKF). */
    val kinderfreibetrag: BigDecimal,

    // --- §9a / §10c EStG: Pauschbetraege ---

    /** Arbeitnehmer-Pauschbetrag (employee flat-rate deduction). */
    val arbeitnehmerPauschbetrag: BigDecimal,
    /** Sonderausgaben-Pauschbetrag (special expenses flat-rate). */
    val sonderausgabenPauschbetrag: BigDecimal,

    // --- Vorsorgepauschale ---

    /** VSPHB: cap for KV/PV component of Vorsorgepauschale. */
    val vsphb: BigDecimal,

    // --- MST5_6 ---

    /** First bracket boundary for Steuerklasse V/VI table calculation. */
    val mst56Bracket1: BigDecimal
)
