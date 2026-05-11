package com.marcoacuna.payroll.application.port.input

import java.time.LocalDate

/** Query for effective rates at a given date. */
data class RatesQuery(val date: LocalDate)
