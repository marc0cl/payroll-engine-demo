package com.marcoacuna.payroll.domain.rates

import java.time.LocalDate

/**
 * Complete set of rates effective for a given period.
 * Versioned by effective date because the BMF publishes PAP updates mid-year
 * (e.g., PAP 2025 January, PAP 2025 March) and SV rates can also change.
 */
data class EffectiveRates(
    /** First day this rate set applies (inclusive). */
    val effectiveFrom: LocalDate,
    /** Last day this rate set applies (inclusive). Null means currently active. */
    val effectiveUntil: LocalDate?,
    /** PAP version identifier (e.g., "PAP2025_Januar", "PAP2025_Maerz"). */
    val papVersion: String,
    /** Social insurance rates (RV, KV, AV). */
    val socialInsurance: SocialInsuranceRates,
    /** Pflegeversicherung rates with child-based differentiation. */
    val pflegeversicherung: PflegeversicherungRates,
    /** Uebergangsbereich parameters (thresholds + F factor). */
    val uebergangsbereich: UebergangsbereichParams,
    /** PAP tariff parameters (§32a EStG zones, Soli thresholds, allowances). */
    val papTariff: PapTariffParams
)
