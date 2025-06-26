package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import jakarta.validation.constraints.NotNull;

public record FraværsPeriode(@NotNull String fom,
                             @NotNull String tom) {
}
