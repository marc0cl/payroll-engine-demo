package com.marcoacuna.payroll.domain.rates

import java.math.BigDecimal

/**
 * Social insurance rates and contribution ceilings for a given period.
 * Since 2025, BBG for RV and AV is national (no West/East split).
 */
data class SocialInsuranceRates(
    /** Rentenversicherung: total rate (e.g., 18.6%), split 50/50 employer/employee. */
    val pensionTotalRate: BigDecimal,
    /** Rentenversicherung: employee share (e.g., 9.3%). */
    val pensionEmployeeRate: BigDecimal,
    /** Beitragsbemessungsgrenze RV: national monthly ceiling. */
    val pensionCeilingMonthly: BigDecimal,

    /** Krankenversicherung: general rate (14.6%), split 50/50. */
    val healthInsuranceGeneralRate: BigDecimal,
    /** KV employee share of the general rate (7.3%). */
    val healthInsuranceEmployeeRate: BigDecimal,
    /** BBG Krankenversicherung: monthly ceiling. */
    val healthInsuranceCeilingMonthly: BigDecimal,

    /** Arbeitslosenversicherung: total rate (e.g., 2.6%), split 50/50. */
    val unemploymentTotalRate: BigDecimal,
    /** AV employee share (e.g., 1.3%). */
    val unemploymentEmployeeRate: BigDecimal,
    /** BBG AV: monthly ceiling (same as RV since unification). */
    val unemploymentCeilingMonthly: BigDecimal
)
