package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record DelvisFraværsPeriode(@NotNull String dato,
                                   @NotNull BigDecimal timer) {
}
