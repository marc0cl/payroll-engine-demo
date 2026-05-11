package com.marcoacuna.payroll.application.usecase

import com.marcoacuna.payroll.application.port.input.RatesQuery
import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.domain.error.PayrollError
import com.marcoacuna.payroll.domain.rates.EffectiveRates
import com.marcoacuna.payroll.domain.rates.RatesProvider

class QueryRatesUseCase(
    private val ratesProvider: RatesProvider
) {

    fun queryByDate(query: RatesQuery): DomainResult<EffectiveRates> {
        val rates = ratesProvider.ratesFor(query.date)
            ?: return DomainResult.Failure(
                PayrollError.UnsupportedCalculationPeriod(
                    query.date,
                    ratesProvider.allPeriods().map { "${it.effectiveFrom} - ${it.effectiveUntil ?: "current"}" }
                )
            )
        return DomainResult.Success(rates)
    }

    fun queryAll(): List<EffectiveRates> = ratesProvider.allPeriods()
}
