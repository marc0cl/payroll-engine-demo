package com.marcoacuna.payroll.infrastructure.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Servlet filter that reads or generates an X-Correlation-Id header,
 * stores it in the SLF4J MDC, and propagates it in the response.
 * Boot 4.0 structured logging automatically includes MDC key-value pairs in JSON output.
 */
@Component
@Order(1)
class CorrelationIdFilter : OncePerRequestFilter() {

    companion object {
        const val HEADER_NAME = "X-Correlation-Id"
        const val MDC_KEY = "correlationId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val correlationId = request.getHeader(HEADER_NAME) ?: UUID.randomUUID().toString()
        MDC.put(MDC_KEY, correlationId)
        response.setHeader(HEADER_NAME, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }
}
