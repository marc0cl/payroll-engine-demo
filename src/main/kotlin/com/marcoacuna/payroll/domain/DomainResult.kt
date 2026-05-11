package com.marcoacuna.payroll.domain

import com.marcoacuna.payroll.domain.error.PayrollError

/** Simple Result type for domain operations. Avoids exceptions for expected failures. */
sealed class DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>()
    data class Failure(val error: PayrollError) : DomainResult<Nothing>()

    fun <R> map(transform: (T) -> R): DomainResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun <R> flatMap(transform: (T) -> DomainResult<R>): DomainResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw IllegalStateException("DomainResult.Failure: $error")
    }
}
