package com.marcoacuna.payroll.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("Payroll Engine — German Net Salary Calculator")
            .version("1.0.0")
            .description(
                """
                Finance-grade payroll calculation engine implementing a subset of the
                BMF Programmablaufplan (PAP) for LZZ=2 (standard monthly salary).

                **Scope:** Lohnsteuer, Solidaritätszuschlag, Kirchensteuer (via PAP),
                Rentenversicherung, Krankenversicherung, Pflegeversicherung (with child-based rates),
                Arbeitslosenversicherung, Übergangsbereich.

                **Calculation mode:** OFFICIAL_PAP_LZZ2

                **Out of scope:** Sonstige Bezüge, Versorgungsbezüge, Altersentlastungsbetrag,
                Mini-jobs, multiple employers.

                **Validation:** Results can be verified against the BMF Lohn- und Einkommensteuerrechner
                at bmf-steuerrechner.de/lst/
                """.trimIndent()
            )
    )
}
