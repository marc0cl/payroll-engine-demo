package com.marcoacuna.payroll.application.usecase

import com.marcoacuna.payroll.application.port.input.CalculateSalaryCommand
import com.marcoacuna.payroll.domain.DomainResult
import com.marcoacuna.payroll.domain.model.PayrollResult
import com.marcoacuna.payroll.domain.service.PayrollCalculationService

class CalculateNetSalaryUseCase(
    private val calculationService: PayrollCalculationService
) {

    fun execute(command: CalculateSalaryCommand): DomainResult<PayrollResult> {
        return command.toEmployeeInput().flatMap { input ->
            calculationService.calculate(input)
        }
    }
}
