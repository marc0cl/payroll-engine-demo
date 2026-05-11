package com.marcoacuna.payroll.infrastructure.adapter.input.rest

import com.marcoacuna.payroll.domain.error.PayrollError
import com.marcoacuna.payroll.infrastructure.adapter.input.rest.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DomainErrorException::class)
    fun handleDomainError(ex: DomainErrorException): ResponseEntity<ErrorResponse> {
        val (status, response) = when (val error = ex.error) {
            is PayrollError.InvalidGrossSalary -> HttpStatus.BAD_REQUEST to ErrorResponse(
                code = "INVALID_GROSS_SALARY", message = error.reason
            )
            is PayrollError.InvalidInput -> HttpStatus.BAD_REQUEST to ErrorResponse(
                code = "INVALID_INPUT", message = error.reason, field = error.field
            )
            is PayrollError.IncompatibleParameters -> HttpStatus.UNPROCESSABLE_ENTITY to ErrorResponse(
                code = "INCOMPATIBLE_PARAMETERS", message = error.details
            )
            is PayrollError.UnsupportedCalculationPeriod -> HttpStatus.NOT_FOUND to ErrorResponse(
                code = "UNSUPPORTED_PERIOD",
                message = "No rates available for ${error.requestedDate}. Available: ${error.availablePeriods}"
            )
            is PayrollError.CalculationOverflow -> HttpStatus.INTERNAL_SERVER_ERROR to ErrorResponse(
                code = "CALCULATION_OVERFLOW", message = error.detail
            )
        }
        return ResponseEntity.status(status).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val firstError = ex.bindingResult.fieldErrors.firstOrNull()
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                code = "INVALID_INPUT",
                message = firstError?.defaultMessage ?: "Validation failed",
                field = firstError?.field
            )
        )
    }
}
