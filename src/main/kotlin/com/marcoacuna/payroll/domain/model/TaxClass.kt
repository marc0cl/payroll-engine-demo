package com.marcoacuna.payroll.domain.model

/**
 * German Steuerklasse (tax class) I through VI.
 * The Grundfreibetrag and other tax-specific parameters are handled
 * internally by the PAP — TaxClass is purely a classifier.
 */
enum class TaxClass(val value: Int) {
    I(1),
    II(2),
    III(3),
    IV(4),
    V(5),
    VI(6);

    companion object {
        fun fromInt(value: Int): TaxClass? = entries.find { it.value == value }
        fun fromString(value: String): TaxClass? = entries.find { it.name == value }
    }
}
