package com.marcoacuna.payroll.infrastructure.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class PayrollMetrics(meterRegistry: MeterRegistry) {

    private val calculationCounter: Counter = Counter.builder("payroll.calculations.total")
        .description("Total number of payroll calculations requested")
        .register(meterRegistry)

    private val errorCounter: Counter = Counter.builder("payroll.calculations.errors")
        .description("Total number of payroll calculation errors")
        .register(meterRegistry)

    private val durationTimer: Timer = Timer.builder("payroll.calculations.duration")
        .description("Duration of payroll calculations")
        .register(meterRegistry)

    fun incrementCalculations() = calculationCounter.increment()
    fun incrementErrors() = errorCounter.increment()
    fun recordDuration(millis: Double) = durationTimer.record(millis.toLong(), TimeUnit.MILLISECONDS)
}
