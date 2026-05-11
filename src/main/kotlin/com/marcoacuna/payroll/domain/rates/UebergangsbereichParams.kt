package com.marcoacuna.payroll.domain.rates

import java.math.BigDecimal

/**
 * Parameters for the Uebergangsbereich (transition zone) calculation.
 * §20 Abs. 2a SGB IV: Employees earning between the Geringfuegigkeitsgrenze
 * and the upper threshold pay reduced social insurance contributions.
 *
 * Formula for reduced beitragspflichtige Einnahme (BE):
 *   BE = F * G + ((T / (T - G)) - (G / (T - G)) * F) * (AE - G)
 * where G = lower threshold, T = upper threshold, AE = actual gross, F = factor.
 */
data class UebergangsbereichParams(
    /** Geringfuegigkeitsgrenze: lower threshold / Minijob ceiling (e.g., 556.00 EUR for 2025, 603.00 EUR for 2026). */
    val lowerThreshold: BigDecimal,
    /** Upper threshold of the transition zone (2000.00 EUR). */
    val upperThreshold: BigDecimal,
    /** F factor published annually by BMAS (e.g., 0.6683 for 2025, 0.6619 for 2026). */
    val factor: BigDecimal
)
