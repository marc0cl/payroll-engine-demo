package com.marcoacuna.payroll.infrastructure.adapter.output

import com.marcoacuna.payroll.domain.rates.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * In-memory rates adapter with real German SV and PAP data.
 * Rates are versioned by effective date to support mid-year PAP revisions.
 *
 * Sources:
 * - RV/AV rates and BBG: Deutsche Rentenversicherung, BMAS
 * - KV rates and BBG: GKV-Spitzenverband
 * - PV rates: §55 SGB XI, PUEG amendments
 * - Uebergangsbereich F-factor: BMAS (Sozialversicherungsrechengroessenverordnung)
 * - PAP versions and §32a tariff: BMF (bmf-steuerrechner.de)
 */
class InMemoryRatesAdapter : RatesProvider {

    companion object {
        /**
         * PAP 2025 March tariff parameters (§32a EStG, as revised by Steuerentlastungsgesetz 2025).
         * The March revision is retroactive to January 1, so both 2025 periods use these values.
         */
        val PAP_2025_TARIFF = PapTariffParams(
            grundfreibetrag = BigDecimal("12096"),
            zone2Upper = BigDecimal("17443"),
            zone3Upper = BigDecimal("66760"),
            zone4Upper = BigDecimal("277825"),
            zone2A = BigDecimal("922.98"),
            zone2B = BigDecimal("1400"),
            zone3A = BigDecimal("181.19"),
            zone3B = BigDecimal("2397"),
            zone3C = BigDecimal("966.53"),
            zone4Rate = BigDecimal("0.42"),
            zone4Sub = BigDecimal("10636.31"),
            zone5Rate = BigDecimal("0.45"),
            zone5Sub = BigDecimal("18971.56"),
            soliFreigrenze = BigDecimal("18130"),
            soliFreigrenzeSTKL3 = BigDecimal("36260"),
            kinderfreibetrag = BigDecimal("6612"),
            arbeitnehmerPauschbetrag = BigDecimal("1230"),
            sonderausgabenPauschbetrag = BigDecimal("36"),
            vsphb = BigDecimal("1900"),
            mst56Bracket1 = BigDecimal("12485")
        )

        // TODO: Replace with values from BMF PAP2026 XML once published.
        // The §32a tariff (Grundfreibetrag, zone boundaries, coefficients) changes annually.
        // Using 2025 tariff as placeholder — this WILL produce incorrect Lohnsteuer for 2026.
        val PAP_2026_TARIFF = PAP_2025_TARIFF
    }

    private val periods: List<EffectiveRates> = listOf(
        // PAP 2025 January (SV rates same as March; tariff uses retroactive March revision)
        EffectiveRates(
            effectiveFrom = LocalDate.of(2025, 1, 1),
            effectiveUntil = LocalDate.of(2025, 2, 28),
            papVersion = "PAP2025_Januar",
            socialInsurance = SocialInsuranceRates(
                pensionTotalRate = BigDecimal("0.186"),
                pensionEmployeeRate = BigDecimal("0.093"),
                pensionCeilingMonthly = BigDecimal("8050.00"),
                healthInsuranceGeneralRate = BigDecimal("0.146"),
                healthInsuranceEmployeeRate = BigDecimal("0.073"),
                healthInsuranceCeilingMonthly = BigDecimal("5512.50"),
                unemploymentTotalRate = BigDecimal("0.026"),
                unemploymentEmployeeRate = BigDecimal("0.013"),
                unemploymentCeilingMonthly = BigDecimal("8050.00")
            ),
            pflegeversicherung = PflegeversicherungRates(
                totalRate = BigDecimal("0.036"),
                employerShare = BigDecimal("0.018"),
                employeeBaseShare = BigDecimal("0.018"),
                childlessSurcharge = BigDecimal("0.006"),
                childReductionPerChild = BigDecimal("0.0025"),
                maxChildrenForReduction = 5,
                childlessSurchargeMinAge = 23,
                childReductionMaxChildAge = 25,
                sachsenEmployeeExtra = BigDecimal("0.005")
            ),
            uebergangsbereich = UebergangsbereichParams(
                lowerThreshold = BigDecimal("556.00"),
                upperThreshold = BigDecimal("2000.00"),
                factor = BigDecimal("0.6683")   // BMAS 2025
            ),
            papTariff = PAP_2025_TARIFF
        ),

        // PAP 2025 March revision
        EffectiveRates(
            effectiveFrom = LocalDate.of(2025, 3, 1),
            effectiveUntil = LocalDate.of(2025, 12, 31),
            papVersion = "PAP2025_Maerz",
            socialInsurance = SocialInsuranceRates(
                pensionTotalRate = BigDecimal("0.186"),
                pensionEmployeeRate = BigDecimal("0.093"),
                pensionCeilingMonthly = BigDecimal("8050.00"),
                healthInsuranceGeneralRate = BigDecimal("0.146"),
                healthInsuranceEmployeeRate = BigDecimal("0.073"),
                healthInsuranceCeilingMonthly = BigDecimal("5512.50"),
                unemploymentTotalRate = BigDecimal("0.026"),
                unemploymentEmployeeRate = BigDecimal("0.013"),
                unemploymentCeilingMonthly = BigDecimal("8050.00")
            ),
            pflegeversicherung = PflegeversicherungRates(
                totalRate = BigDecimal("0.036"),
                employerShare = BigDecimal("0.018"),
                employeeBaseShare = BigDecimal("0.018"),
                childlessSurcharge = BigDecimal("0.006"),
                childReductionPerChild = BigDecimal("0.0025"),
                maxChildrenForReduction = 5,
                childlessSurchargeMinAge = 23,
                childReductionMaxChildAge = 25,
                sachsenEmployeeExtra = BigDecimal("0.005")
            ),
            uebergangsbereich = UebergangsbereichParams(
                lowerThreshold = BigDecimal("556.00"),
                upperThreshold = BigDecimal("2000.00"),
                factor = BigDecimal("0.6683")   // BMAS 2025
            ),
            papTariff = PAP_2025_TARIFF
        ),

        // 2026 — SV values per BMAS Sozialversicherungsrechengroessenverordnung 2026
        EffectiveRates(
            effectiveFrom = LocalDate.of(2026, 1, 1),
            effectiveUntil = null,
            papVersion = "PAP2026",
            socialInsurance = SocialInsuranceRates(
                pensionTotalRate = BigDecimal("0.186"),
                pensionEmployeeRate = BigDecimal("0.093"),
                pensionCeilingMonthly = BigDecimal("8450.00"),    // BBG RV 2026
                healthInsuranceGeneralRate = BigDecimal("0.146"),
                healthInsuranceEmployeeRate = BigDecimal("0.073"),
                healthInsuranceCeilingMonthly = BigDecimal("5812.50"),  // BBG KV 2026
                unemploymentTotalRate = BigDecimal("0.026"),
                unemploymentEmployeeRate = BigDecimal("0.013"),
                unemploymentCeilingMonthly = BigDecimal("8450.00")  // BBG AV 2026
            ),
            pflegeversicherung = PflegeversicherungRates(
                totalRate = BigDecimal("0.036"),
                employerShare = BigDecimal("0.018"),
                employeeBaseShare = BigDecimal("0.018"),
                childlessSurcharge = BigDecimal("0.006"),
                childReductionPerChild = BigDecimal("0.0025"),
                maxChildrenForReduction = 5,
                childlessSurchargeMinAge = 23,
                childReductionMaxChildAge = 25,
                sachsenEmployeeExtra = BigDecimal("0.005")
            ),
            uebergangsbereich = UebergangsbereichParams(
                lowerThreshold = BigDecimal("603.00"),    // Geringfuegigkeitsgrenze 2026
                upperThreshold = BigDecimal("2000.00"),
                factor = BigDecimal("0.6619")             // BMAS 2026
            ),
            papTariff = PAP_2026_TARIFF
        )
    )

    override fun ratesFor(date: LocalDate): EffectiveRates? =
        periods.find { rate ->
            !date.isBefore(rate.effectiveFrom) &&
                (rate.effectiveUntil == null || !date.isAfter(rate.effectiveUntil))
        }

    override fun allPeriods(): List<EffectiveRates> =
        periods.sortedBy { it.effectiveFrom }
}
