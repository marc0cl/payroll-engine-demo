package com.marcoacuna.payroll.domain.audit

import com.marcoacuna.payroll.domain.model.CalculationMode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Auditable trace of a complete payroll calculation.
 * Contains the PAP version used, the effective rates date, all calculation steps,
 * and enough information to reproduce the calculation.
 */
data class CalculationTrace(
    val traceId: UUID,
    val timestamp: Instant,
    val calculationMode: CalculationMode,
    val papVersion: String,
    val ratesEffectiveFrom: LocalDate,
    val engineVersion: String,
    val bpiSV: BigDecimal,
    val isUebergangsbereich: Boolean,
    val capsApplied: List<String>,
    val steps: List<CalculationStep>
)
