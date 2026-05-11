package com.marcoacuna.payroll.infrastructure.adapter.input.rest

import com.marcoacuna.payroll.domain.error.PayrollError

/** Bridge between domain errors (sealed class) and Spring's exception-based error handling. */
class DomainErrorException(val error: PayrollError) : RuntimeException(error.toString())
