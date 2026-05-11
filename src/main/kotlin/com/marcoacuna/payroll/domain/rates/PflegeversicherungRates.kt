package com.marcoacuna.payroll.domain.rates

import java.math.BigDecimal

/**
 * Pflegeversicherung (care insurance) rates, differentiated by number of children.
 * Based on §55 Abs. 3 SGB XI as amended by PUEG (effective 1 July 2023).
 *
 * Rate structure:
 * - Childless employees >= 23: base employee rate + childlessSurcharge
 * - 1 child: base employee rate
 * - 2-5 children under 25: base employee rate - (childReductionPerChild * (n-1))
 * - Sachsen: employee pays extra sachsenEmployeeExtra, employer pays less
 */
data class PflegeversicherungRates(
    /** Total PV rate (e.g., 3.6% for 2025). */
    val totalRate: BigDecimal,
    /** Standard employer share (e.g., 1.8%). */
    val employerShare: BigDecimal,
    /** Standard employee base share (e.g., 1.8%). */
    val employeeBaseShare: BigDecimal,
    /** Kinderlosenzuschlag: surcharge for childless employees >= 23 (e.g., 0.6%). */
    val childlessSurcharge: BigDecimal,
    /** Reduction per child from 2nd to maxChildren, each under childReductionMaxChildAge (e.g., 0.25%). */
    val childReductionPerChild: BigDecimal,
    /** Maximum number of children counted for reduction (5). */
    val maxChildrenForReduction: Int,
    /** Minimum age for childless surcharge to apply (23). */
    val childlessSurchargeMinAge: Int,
    /** Maximum age of child to count for reduction (25). */
    val childReductionMaxChildAge: Int,
    /** Sachsen extra: additional employee share in Sachsen (e.g., 0.5%). */
    val sachsenEmployeeExtra: BigDecimal
)
