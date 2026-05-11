package com.marcoacuna.payroll.domain.rates

import java.time.LocalDate

/** Port for retrieving effective rates. Implementation lives in infrastructure layer. */
interface RatesProvider {
    /** Returns the rates effective on the given date, or null if no rates cover that date. */
    fun ratesFor(date: LocalDate): EffectiveRates?

    /** Returns all available rate periods, ordered by effectiveFrom ascending. */
    fun allPeriods(): List<EffectiveRates>
}
