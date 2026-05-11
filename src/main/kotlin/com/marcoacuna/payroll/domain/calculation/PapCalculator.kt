package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.audit.CalculationStep
import com.marcoacuna.payroll.domain.model.Deduction
import com.marcoacuna.payroll.domain.model.DeductionType
import com.marcoacuna.payroll.domain.rates.PapTariffParams
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Implementation of the BMF Programmablaufplan (PAP) for LZZ=2 (monthly standard salary).
 *
 * This calculator follows the official PAP pseudocode step by step.
 * Variable names (ZRE4, ZTABFB, VSPN, etc.) match the PAP specification.
 *
 * IMPORTANT: Rounding inside the PAP follows per-field rules from the specification.
 * Intermediate BigDecimal values carry extra precision; truncation/rounding happens
 * only where the PAP specifies it (typically at assignment to cent fields).
 *
 * Outputs: LSTLZZ (Lohnsteuer), SOLZLZZ (Solidaritaetszuschlag), BK (KiSt basis).
 *
 * This is a subset: LZZ=2 only, no SONSTB, no VBEZ, no ALTER1/AJAHR.
 *
 * Reference: PAP 2025/2026, §39b EStG, §32a EStG, §3 SolZG.
 */
class PapCalculator : DeductionCalculator {

    // --- PAP internal variables ---
    // Named exactly as in the PAP specification for traceability.

    private var zre4: BigDecimal = BigDecimal.ZERO      // Auf den LZZ umgerechneter RE4
    private var zre4vp: BigDecimal = BigDecimal.ZERO     // Fuer Vorsorgepauschale
    private var zvbez: BigDecimal = BigDecimal.ZERO      // Versorgungsbezuege (0 in our scope)
    private var ztabfb: BigDecimal = BigDecimal.ZERO     // Tabellenfreibetrag
    private var zve: BigDecimal = BigDecimal.ZERO        // Zu versteuerndes Einkommen
    private var st: BigDecimal = BigDecimal.ZERO         // Steuerbetrag
    private var lstjahr: BigDecimal = BigDecimal.ZERO    // Jahres-Lohnsteuer
    private var lstlzz: BigDecimal = BigDecimal.ZERO     // LZZ-Lohnsteuer (output)
    private var solzlzz: BigDecimal = BigDecimal.ZERO    // LZZ-Soli (output)
    private var bk: BigDecimal = BigDecimal.ZERO         // KiSt-Bemessungsgrundlage (output)

    // Vorsorgepauschale components
    private var vsp: BigDecimal = BigDecimal.ZERO        // Vorsorgepauschale total
    private var vspalv: BigDecimal = BigDecimal.ZERO     // RV component
    private var vspkvpv: BigDecimal = BigDecimal.ZERO    // KV/PV component

    // Tariff params — loaded from EffectiveRates per PAP version
    private lateinit var tariff: PapTariffParams

    private lateinit var papInput: PapInput

    // SV contributions from context (for Vorsorgepauschale)
    private var rvContribution: BigDecimal = BigDecimal.ZERO
    private var kvContribution: BigDecimal = BigDecimal.ZERO
    private var pvContribution: BigDecimal = BigDecimal.ZERO

    override fun calculate(context: CalculationContext) {
        papInput = PapInputMapper.map(context.input)
        tariff = context.rates.papTariff

        // Carry SV contributions from previous pipeline steps
        rvContribution = context.pensionContribution
        kvContribution = context.healthInsuranceContribution
        pvContribution = context.careInsuranceContribution

        // PAP Main flow for LZZ=2
        mre4()          // Step 1: Prepare annual income
        mztabfb()       // Step 2: Calculate Tabellenfreibetrag (includes Vorsorgepauschale)
        mlstjahr()      // Step 3: Calculate annual Lohnsteuer
        lstlzzCalc()    // Step 4: Monthly Lohnsteuer
        msolz()         // Step 5: Solidaritaetszuschlag
        bkCalc()        // Step 6: KiSt Bemessungsgrundlage

        // Write results to context
        context.lohnsteuer = lstlzz
        context.solidaritaetszuschlag = solzlzz
        context.kirchensteuerBasis = bk

        // Add Lohnsteuer deduction
        context.deductions.add(
            Deduction(
                DeductionType.LOHNSTEUER,
                context.input.grossMonthlySalary.amount,
                lstlzz.divide(context.input.grossMonthlySalary.amount.max(BigDecimal.ONE), 10, RoundingMode.HALF_UP),
                lstlzz,
                "§39b EStG (PAP LZZ=2)"
            )
        )
        context.steps.add(
            CalculationStep(
                DeductionType.LOHNSTEUER,
                context.input.grossMonthlySalary.amount,
                lstlzz.divide(context.input.grossMonthlySalary.amount.max(BigDecimal.ONE), 10, RoundingMode.HALF_UP),
                lstlzz,
                "§39b EStG, §32a EStG",
                "PAP per-field truncation",
                "PAP LZZ=2, STKL=${papInput.stkl}, Vorsorgepauschale=$vsp, ZVE=$zve"
            )
        )

        // Add Soli deduction
        context.deductions.add(
            Deduction(
                DeductionType.SOLIDARITAETSZUSCHLAG,
                lstlzz,
                if (solzlzz > BigDecimal.ZERO) BigDecimal("0.055") else BigDecimal.ZERO,
                solzlzz,
                "§3 SolZG"
            )
        )
        context.steps.add(
            CalculationStep(
                DeductionType.SOLIDARITAETSZUSCHLAG,
                lstlzz,
                if (solzlzz > BigDecimal.ZERO) BigDecimal("0.055") else BigDecimal.ZERO,
                solzlzz,
                "§3, §4 SolZG",
                "PAP per-field truncation",
                "Soli on Jahres-LSt=$lstjahr"
            )
        )
    }

    /**
     * MRE4: Prepare Jahresarbeitslohn from LZZ income.
     * For LZZ=2 (monthly): annual = monthly * 12.
     */
    private fun mre4() {
        // RE4 is in cents, convert to euros
        val re4Euro = papInput.re4.divide(BigDecimal(100), 2, RoundingMode.DOWN)

        // Annualize: LZZ=2 means monthly, so multiply by 12
        zre4 = re4Euro.multiply(BigDecimal(12))
        zre4vp = zre4

        // Add LZZHINZU (also annualize)
        val hinzuEuro = papInput.lzzhinzu.divide(BigDecimal(100), 2, RoundingMode.DOWN)
        zre4 = zre4.add(hinzuEuro.multiply(BigDecimal(12)))
    }

    /**
     * MZTABFB: Calculate Tabellenfreibetrag.
     * Includes: Grundfreibetrag, Arbeitnehmer-Pauschbetrag, Sonderausgaben-Pauschbetrag,
     * Vorsorgepauschale, and LZZFREIB.
     */
    private fun mztabfb() {
        // Compute Vorsorgepauschale first
        upevs()

        // Grundfreibetrag: doubled for STKL III
        val gfb = when (papInput.stkl) {
            3 -> tariff.grundfreibetrag.multiply(BigDecimal(2))
            else -> tariff.grundfreibetrag
        }

        // Arbeitnehmer-Pauschbetrag, not for STKL VI
        val anpb = if (papInput.stkl == 6) BigDecimal.ZERO else tariff.arbeitnehmerPauschbetrag

        // Sonderausgaben-Pauschbetrag, not for STKL VI
        val sapb = if (papInput.stkl == 6) BigDecimal.ZERO else tariff.sonderausgabenPauschbetrag

        // LZZFREIB annualized
        val freib = papInput.lzzfreib.divide(BigDecimal(100), 2, RoundingMode.DOWN)
            .multiply(BigDecimal(12))

        ztabfb = gfb.add(anpb).add(sapb).add(vsp).add(freib)
    }

    /**
     * UPEVS: Vorsorgepauschale calculation.
     * Components: VSPALV (RV), VSPKVPV (KV+PV).
     * STKL 6: Vorsorgepauschale = 0.
     */
    private fun upevs() {
        if (papInput.stkl == 6) {
            vsp = BigDecimal.ZERO
            return
        }

        // VSPALV: RV component = annual RV contribution (employee share)
        vspalv = rvContribution.multiply(BigDecimal(12))

        // VSPKVPV: KV + PV component
        if (papInput.pkv == 0) {
            // GKV: use actual KV + PV contributions
            val kvAnnual = kvContribution.multiply(BigDecimal(12))
            val pvAnnual = pvContribution.multiply(BigDecimal(12))
            vspkvpv = kvAnnual.add(pvAnnual)
        } else {
            // PKV: use PKPV (already annual in PapInput)
            val pkpvEuro = papInput.pkpv.divide(BigDecimal(100), 2, RoundingMode.DOWN)
            vspkvpv = pkpvEuro
            // Cap for PKV
            if (papInput.pkv == 2) {
                // With AG-Zuschuss: halve the amount (employer pays half)
                vspkvpv = vspkvpv.divide(BigDecimal(2), 2, RoundingMode.UP)
            }
        }

        // Cap VSPKVPV at VSPHB
        vspkvpv = vspkvpv.min(tariff.vsphb)

        // Also compare with 12% of gross (simplified PAP rule)
        val vspMaxPercent = zre4vp.multiply(BigDecimal("0.12"))
        vspkvpv = vspkvpv.min(vspMaxPercent)

        vsp = vspalv.add(vspkvpv).max(BigDecimal.ZERO)
    }

    /**
     * MLSTJAHR: Annual income tax calculation per §32a EStG.
     * Uses the progressive tariff with quadratic zones.
     *
     * For STKL V and VI: uses the MST5_6 special table.
     * For STKL IV with AF=1: applies the Factor.
     */
    private fun mlstjahr() {
        // Zu versteuerndes Einkommen
        zve = zre4.subtract(ztabfb).max(BigDecimal.ZERO)
        // Truncate to full euros (PAP: "Abrundung auf vollen Euro-Betrag")
        zve = zve.setScale(0, RoundingMode.DOWN)

        when (papInput.stkl) {
            5, 6 -> mst56()
            else -> {
                st = tarif32a(zve)
                // STKL III: tariff applied to half ZVE, then doubled
                if (papInput.stkl == 3) {
                    val halfZve = zve.divide(BigDecimal(2), 0, RoundingMode.DOWN)
                    st = tarif32a(halfZve).multiply(BigDecimal(2))
                }
            }
        }

        lstjahr = st

        // Faktorverfahren: STKL IV with AF=1
        if (papInput.stkl == 4 && papInput.af == 1) {
            lstjahr = lstjahr.multiply(papInput.f).setScale(0, RoundingMode.DOWN)
        }

        // Kinderfreibetrag adjustment for Soli/KiSt
        // (affects BK and Soli calculation, not Lohnsteuer itself)
    }

    /**
     * §32a EStG tariff: progressive income tax with quadratic zones.
     *
     * Zone 1: 0 to grundfreibetrag → 0
     * Zone 2: grundfreibetrag+1 to zone2Upper → progressive, quadratic
     * Zone 3: zone2Upper+1 to zone3Upper → progressive, quadratic
     * Zone 4: zone3Upper+1 to zone4Upper → Spitzensteuersatz
     * Zone 5: above zone4Upper → Reichensteuer
     *
     * All coefficients sourced from [tariff] (version-specific).
     */
    private fun tarif32a(zvE: BigDecimal): BigDecimal {
        if (zvE <= tariff.grundfreibetrag) {
            return BigDecimal.ZERO
        }

        if (zvE <= tariff.zone2Upper) {
            val y = zvE.subtract(tariff.grundfreibetrag).divide(BigDecimal("10000"), 10, RoundingMode.DOWN)
            val est = tariff.zone2A.multiply(y)
                .add(tariff.zone2B)
                .multiply(y)
            return est.setScale(0, RoundingMode.DOWN)
        }

        if (zvE <= tariff.zone3Upper) {
            val z = zvE.subtract(tariff.zone2Upper).divide(BigDecimal("10000"), 10, RoundingMode.DOWN)
            val est = tariff.zone3A.multiply(z)
                .add(tariff.zone3B)
                .multiply(z)
                .add(tariff.zone3C)
            return est.setScale(0, RoundingMode.DOWN)
        }

        if (zvE <= tariff.zone4Upper) {
            val est = zvE.multiply(tariff.zone4Rate).subtract(tariff.zone4Sub)
            return est.setScale(0, RoundingMode.DOWN)
        }

        val est = zvE.multiply(tariff.zone5Rate).subtract(tariff.zone5Sub)
        return est.setScale(0, RoundingMode.DOWN)
    }

    /**
     * MST5_6: Special calculation for Steuerklassen V and VI.
     * Uses a lookup table with specific marginal rates.
     * Simplified implementation using the PAP's described bracket table.
     * Brackets derived from version-specific [tariff] parameters.
     */
    private fun mst56() {
        val brackets = listOf(
            Pair(tariff.mst56Bracket1, BigDecimal("0.14")),
            Pair(tariff.zone2Upper, BigDecimal("0.2397")),
            Pair(tariff.zone3Upper, tariff.zone4Rate),
            Pair(tariff.zone4Upper, tariff.zone4Rate),
            Pair(null, tariff.zone5Rate)
        )

        var remaining = zve
        var tax = BigDecimal.ZERO
        var prevBound = BigDecimal.ZERO

        for ((upper, rate) in brackets) {
            if (remaining <= BigDecimal.ZERO) break
            val limit = if (upper != null) upper.subtract(prevBound) else remaining
            val taxable = remaining.min(limit)
            tax = tax.add(taxable.multiply(rate))
            remaining = remaining.subtract(taxable)
            prevBound = upper ?: prevBound
        }

        st = tax.setScale(0, RoundingMode.DOWN)
    }

    /**
     * Compute LSTLZZ: monthly Lohnsteuer from annual.
     * For LZZ=2: LSTLZZ = LSTJAHR / 12, truncated to full cents (down).
     */
    private fun lstlzzCalc() {
        lstlzz = lstjahr.divide(BigDecimal(12), 2, RoundingMode.DOWN)
        // Floor at zero
        lstlzz = lstlzz.max(BigDecimal.ZERO)
    }

    /**
     * MSOLZ: Solidaritaetszuschlag.
     * 5.5% of annual Lohnsteuer if above the exemption threshold.
     * Below threshold: Soli = 0.
     * Mitigation zone: linear phase-in above threshold (§4 SolZG).
     */
    private fun msolz() {
        val threshold = when (papInput.stkl) {
            3 -> tariff.soliFreigrenzeSTKL3
            else -> tariff.soliFreigrenze
        }

        if (lstjahr <= threshold) {
            solzlzz = BigDecimal.ZERO
            return
        }

        // Full Soli: 5.5% of annual Lohnsteuer
        val solzJahr = lstjahr.multiply(BigDecimal("0.055"))

        // Mitigation zone: Soli cannot exceed 11.9% of the amount above threshold
        val mitigation = lstjahr.subtract(threshold).multiply(BigDecimal("0.119"))
        val effectiveSolzJahr = solzJahr.min(mitigation)

        // Monthly
        solzlzz = effectiveSolzJahr.divide(BigDecimal(12), 2, RoundingMode.DOWN)
        solzlzz = solzlzz.max(BigDecimal.ZERO)
    }

    /**
     * BK: Bemessungsgrundlage for Kirchensteuer.
     * Computed similarly to Lohnsteuer but with Kinderfreibetraege applied
     * (Kinderfreibetraege reduce the basis for KiSt but not for Lohnsteuer itself).
     */
    private fun bkCalc() {
        if (papInput.r == 0) {
            bk = BigDecimal.ZERO
            return
        }

        // Kinderfreibetrag per ZKF, doubled for STKL III
        val kfb = when (papInput.stkl) {
            3 -> papInput.zkf.multiply(tariff.kinderfreibetrag).multiply(BigDecimal(2))
            else -> papInput.zkf.multiply(tariff.kinderfreibetrag)
        }

        val zveKist = zve.subtract(kfb).max(BigDecimal.ZERO)
            .setScale(0, RoundingMode.DOWN)

        val stKist = when (papInput.stkl) {
            3 -> {
                val halfZve = zveKist.divide(BigDecimal(2), 0, RoundingMode.DOWN)
                tarif32a(halfZve).multiply(BigDecimal(2))
            }
            5, 6 -> {
                // For BK in STKL V/VI, recalculate with adjusted ZVE
                val savedZve = zve
                zve = zveKist
                mst56()
                zve = savedZve
                st
            }
            else -> tarif32a(zveKist)
        }

        // Factor for STKL IV
        val effectiveStKist = if (papInput.stkl == 4 && papInput.af == 1) {
            stKist.multiply(papInput.f).setScale(0, RoundingMode.DOWN)
        } else {
            stKist
        }

        // BK = monthly KiSt basis
        bk = effectiveStKist.divide(BigDecimal(12), 2, RoundingMode.DOWN)
        bk = bk.max(BigDecimal.ZERO)
    }
}
