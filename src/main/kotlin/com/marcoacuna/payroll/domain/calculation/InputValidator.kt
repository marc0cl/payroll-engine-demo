package com.marcoacuna.payroll.domain.calculation

import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.domain.error.PayrollError
import com.marcoacuna.payroll.domain.model.EmployeeInput
import com.marcoacuna.payroll.domain.model.TaxClass
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period

/**
 * Pre-PAP input validation.
 * The BMF states that plausibility must be ensured in the employer's preprograms,
 * not inside the PAP itself. These rules enforce that contract.
 */
object InputValidator {

    fun validate(input: EmployeeInput): DomainResult<EmployeeInput> {
        // AF only valid for STKL IV
        if (input.af && input.stkl != TaxClass.IV) {
            return failure("af", "Faktorverfahren (AF=1) is only valid for Steuerklasse IV")
        }

        // F required if AF=true
        if (input.af && input.f == null) {
            return failure("f", "Factor (F) is required when Faktorverfahren is active (AF=1)")
        }

        // F prohibited if AF=false
        if (!input.af && input.f != null) {
            return failure("f", "Factor (F) must be null when Faktorverfahren is inactive (AF=0)")
        }

        // F range: (0.000, 1.000]
        if (input.f != null) {
            if (input.f <= BigDecimal.ZERO || input.f > BigDecimal.ONE) {
                return failure("f", "Factor (F) must be in range (0.000, 1.000], got: ${input.f}")
            }
            if (input.f.scale() > 3) {
                return failure("f", "Factor (F) must have at most 3 decimal places, got: ${input.f}")
            }
        }

        // ZKF only in STKL I-IV, must be 0 in V/VI
        if (input.stkl in listOf(TaxClass.V, TaxClass.VI) && input.zkf > BigDecimal.ZERO) {
            return failure("zkf", "Kinderfreibetraege (ZKF) must be 0 for Steuerklasse ${input.stkl}")
        }

        // ZKF >= 0
        if (input.zkf < BigDecimal.ZERO) {
            return failure("zkf", "Kinderfreibetraege (ZKF) cannot be negative")
        }

        // LZZFREIB >= 0
        if (input.lzzfreib < BigDecimal.ZERO) {
            return failure("lzzfreib", "LZZFREIB cannot be negative")
        }

        // LZZHINZU >= 0
        if (input.lzzhinzu < BigDecimal.ZERO) {
            return failure("lzzhinzu", "LZZHINZU cannot be negative")
        }

        // Freibetrag and Faktor are mutually exclusive
        if (input.af && input.lzzfreib > BigDecimal.ZERO) {
            return DomainResult.Failure(
                PayrollError.IncompatibleParameters(
                    "LZZFREIB must be 0 when Faktorverfahren is active (Freibetrag and Faktor are mutually exclusive)"
                )
            )
        }

        // KVZ >= 0
        if (input.kvz < BigDecimal.ZERO) {
            return failure("kvz", "Zusatzbeitragssatz (KVZ) cannot be negative")
        }

        // KRV in {0, 1, 2}
        if (input.krv !in listOf(0, 1, 2)) {
            return failure("krv", "KRV must be 0, 1, or 2, got: ${input.krv}")
        }

        // PKV in {0, 1, 2}
        if (input.pkv !in listOf(0, 1, 2)) {
            return failure("pkv", "PKV must be 0, 1, or 2, got: ${input.pkv}")
        }

        // PKPV >= 0, only relevant if PKV > 0
        if (input.pkpv < BigDecimal.ZERO) {
            return failure("pkpv", "PKPV cannot be negative")
        }

        // PVZ age check: if PVZ=true, employee must be >= 23
        if (input.pvz) {
            val age = Period.between(input.birthDate, input.calculationDate).years
            if (age < 23) {
                return DomainResult.Failure(
                    PayrollError.IncompatibleParameters(
                        "PVZ=true but employee is only $age years old (must be >= 23 for Kinderlosenzuschlag)"
                    )
                )
            }
        }

        // numberOfChildrenUnder25 >= 0
        if (input.numberOfChildrenUnder25 < 0) {
            return failure("numberOfChildrenUnder25", "Cannot be negative")
        }

        // Steuerklasse VI: no Kinderfreibetraege, no Freibetrag
        if (input.stkl == TaxClass.VI) {
            if (input.lzzfreib > BigDecimal.ZERO) {
                return failure("lzzfreib", "LZZFREIB must be 0 for Steuerklasse VI")
            }
        }

        // R in {0, 1}
        if (input.r !in listOf(0, 1)) {
            return failure("r", "R must be 0 or 1, got: ${input.r}")
        }

        return DomainResult.Success(input)
    }

    private fun failure(field: String, reason: String): DomainResult<EmployeeInput> =
        DomainResult.Failure(PayrollError.InvalidInput(field, reason))
}
