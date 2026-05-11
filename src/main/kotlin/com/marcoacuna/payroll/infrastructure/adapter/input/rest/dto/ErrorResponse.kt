package com.marcoacuna.payroll.infrastructure.adapter.input.rest.dto

import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val field: String? = null,
    val timestamp: Instant = Instant.now()
)
