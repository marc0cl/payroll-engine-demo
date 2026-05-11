package com.marcoacuna.payroll.domain.model

/** Types of payroll deductions modeled by this engine. */
enum class DeductionType(val germanName: String, val legalBasis: String) {
    RENTENVERSICHERUNG("Rentenversicherung", "§158 SGB VI"),
    KRANKENVERSICHERUNG("Krankenversicherung", "§223 SGB V"),
    PFLEGEVERSICHERUNG("Pflegeversicherung", "§55 SGB XI"),
    ARBEITSLOSENVERSICHERUNG("Arbeitslosenversicherung", "§341 SGB III"),
    LOHNSTEUER("Lohnsteuer", "§39b EStG"),
    SOLIDARITAETSZUSCHLAG("Solidaritätszuschlag", "§3 SolZG"),
    KIRCHENSTEUER("Kirchensteuer", "KiStG");
}
