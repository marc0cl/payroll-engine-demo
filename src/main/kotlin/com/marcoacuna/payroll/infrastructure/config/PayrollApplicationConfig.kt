package com.marcoacuna.payroll.infrastructure.config

import com.marcoacuna.payroll.application.usecase.CalculateNetSalaryUseCase
import com.marcoacuna.payroll.application.usecase.QueryRatesUseCase
import com.marcoacuna.payroll.domain.rates.RatesProvider
import com.marcoacuna.payroll.domain.service.PayrollCalculationService
import com.marcoacuna.payroll.infrastructure.adapter.output.InMemoryRatesAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PayrollApplicationConfig {

    @Bean
    fun ratesProvider(): RatesProvider = InMemoryRatesAdapter()

    @Bean
    fun payrollCalculationService(ratesProvider: RatesProvider): PayrollCalculationService =
        PayrollCalculationService(ratesProvider)

    @Bean
    fun calculateNetSalaryUseCase(service: PayrollCalculationService): CalculateNetSalaryUseCase =
        CalculateNetSalaryUseCase(service)

    @Bean
    fun queryRatesUseCase(ratesProvider: RatesProvider): QueryRatesUseCase =
        QueryRatesUseCase(ratesProvider)
}
