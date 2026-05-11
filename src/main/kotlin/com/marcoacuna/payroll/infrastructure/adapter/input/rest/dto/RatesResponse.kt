package com.marcoacuna.payroll.infrastructure.adapter.input.rest.dto

import com.marcoacuna.payroll.domain.rates.EffectiveRates
import java.math.BigDecimal
import java.time.LocalDate

data class RatesResponse(
    val effectiveFrom: LocalDate,
    val effectiveUntil: LocalDate?,
    val papVersion: String,
    val socialInsurance: SocialInsuranceDto,
    val pflegeversicherung: PflegeversicherungDto,
    val uebergangsbereich: UebergangsbereichDto
) {
    companion object {
        fun from(rates: EffectiveRates): RatesResponse = RatesResponse(
            effectiveFrom = rates.effectiveFrom,
            effectiveUntil = rates.effectiveUntil,
            papVersion = rates.papVersion,
            socialInsurance = SocialInsuranceDto(
                pensionEmployeeRate = rates.socialInsurance.pensionEmployeeRate,
                pensionCeilingMonthly = rates.socialInsurance.pensionCeilingMonthly,
                healthInsuranceEmployeeRate = rates.socialInsurance.healthInsuranceEmployeeRate,
                healthInsuranceCeilingMonthly = rates.socialInsurance.healthInsuranceCeilingMonthly,
                unemploymentEmployeeRate = rates.socialInsurance.unemploymentEmployeeRate,
                unemploymentCeilingMonthly = rates.socialInsurance.unemploymentCeilingMonthly
            ),
            pflegeversicherung = PflegeversicherungDto(
                totalRate = rates.pflegeversicherung.totalRate,
                employeeBaseShare = rates.pflegeversicherung.employeeBaseShare,
                childlessSurcharge = rates.pflegeversicherung.childlessSurcharge,
                childReductionPerChild = rates.pflegeversicherung.childReductionPerChild,
                maxChildrenForReduction = rates.pflegeversicherung.maxChildrenForReduction
            ),
            uebergangsbereich = UebergangsbereichDto(
                lowerThreshold = rates.uebergangsbereich.lowerThreshold,
                upperThreshold = rates.uebergangsbereich.upperThreshold,
                factor = rates.uebergangsbereich.factor
            )
        )
    }
}

data class SocialInsuranceDto(
    val pensionEmployeeRate: BigDecimal,
    val pensionCeilingMonthly: BigDecimal,
    val healthInsuranceEmployeeRate: BigDecimal,
    val healthInsuranceCeilingMonthly: BigDecimal,
    val unemploymentEmployeeRate: BigDecimal,
    val unemploymentCeilingMonthly: BigDecimal
)

data class PflegeversicherungDto(
    val totalRate: BigDecimal,
    val employeeBaseShare: BigDecimal,
    val childlessSurcharge: BigDecimal,
    val childReductionPerChild: BigDecimal,
    val maxChildrenForReduction: Int
)

data class UebergangsbereichDto(
    val lowerThreshold: BigDecimal,
    val upperThreshold: BigDecimal,
    val factor: BigDecimal
)
