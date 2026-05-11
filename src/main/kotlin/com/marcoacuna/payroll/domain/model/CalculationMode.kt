package com.marcoacuna.payroll.domain.model

/**
 * Identifies which calculation algorithm was used.
 * OFFICIAL_PAP_LZZ2: BMF Programmablaufplan for LZZ=2 (standard monthly salary).
 * This is a subset of the full PAP — see CLAUDE.md for scope documentation.
 */
enum class CalculationMode(val description: String) {
    OFFICIAL_PAP_LZZ2("BMF PAP subset for laufender Monatslohn (LZZ=2)");
}
