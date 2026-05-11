package com.marcoacuna.payroll.domain.model

import java.math.BigDecimal

/**
 * German Bundesland (federal state).
 * Each state has a Kirchensteuer rate: 8% for Baden-Wuerttemberg and Bayern, 9% for all others.
 * Sachsen has a special Pflegeversicherung split (PVS flag in PAP).
 */
enum class FederalState(
    val kirchensteuerRate: BigDecimal,
    val isSachsen: Boolean = false
) {
    BADEN_WUERTTEMBERG(BigDecimal("0.08")),
    BAYERN(BigDecimal("0.08")),
    BERLIN(BigDecimal("0.09")),
    BRANDENBURG(BigDecimal("0.09")),
    BREMEN(BigDecimal("0.09")),
    HAMBURG(BigDecimal("0.09")),
    HESSEN(BigDecimal("0.09")),
    MECKLENBURG_VORPOMMERN(BigDecimal("0.09")),
    NIEDERSACHSEN(BigDecimal("0.09")),
    NORDRHEIN_WESTFALEN(BigDecimal("0.09")),
    RHEINLAND_PFALZ(BigDecimal("0.09")),
    SAARLAND(BigDecimal("0.09")),
    SACHSEN(BigDecimal("0.09"), isSachsen = true),
    SACHSEN_ANHALT(BigDecimal("0.09")),
    SCHLESWIG_HOLSTEIN(BigDecimal("0.09")),
    THUERINGEN(BigDecimal("0.09"));
}
