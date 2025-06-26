package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record FraværsInfo(@NotEmpty FraværsPeriode fraværsPeriode,
                          @NotNull Boolean harUtbetaltLønn) {
}
