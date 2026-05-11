package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.model.*
import com.marcoacuna.payroll.domain.rates.*
import java.math.BigDecimal
import java.time.LocalDate

/** Shared test fixtures for domain calculator tests. */

fun testRates(): EffectiveRates = EffectiveRates(
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
        factor = BigDecimal("0.6683")  // BMAS 2025
    ),
    papTariff = PapTariffParams(
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
)

fun testInput(
    gross: BigDecimal = BigDecimal("4000.00"),
    stkl: TaxClass = TaxClass.I,
    af: Boolean = false,
    f: BigDecimal? = null,
    zkf: BigDecimal = BigDecimal.ZERO,
    r: Int = 0,
    kvz: BigDecimal = BigDecimal("2.50"),
    krv: Int = 0,
    pkv: Int = 0,
    pkpv: BigDecimal = BigDecimal.ZERO,
    pvs: Boolean = false,
    pvz: Boolean = false,
    federalState: FederalState = FederalState.BAYERN,
    numberOfChildrenUnder25: Int = 0,
    birthDate: LocalDate = LocalDate.of(1990, 5, 15),
    calculationDate: LocalDate = LocalDate.of(2025, 6, 15)
): EmployeeInput = EmployeeInput(
    grossMonthlySalary = GrossSalary.of(gross).getOrThrow(),
    stkl = stkl,
    af = af,
    f = f,
    zkf = zkf,
    kvz = kvz,
    krv = krv,
    pkv = pkv,
    pkpv = pkpv,
    pvs = pvs,
    pvz = pvz,
    r = r,
    federalState = federalState,
    numberOfChildrenUnder25 = numberOfChildrenUnder25,
    birthDate = birthDate,
    calculationDate = calculationDate
)
