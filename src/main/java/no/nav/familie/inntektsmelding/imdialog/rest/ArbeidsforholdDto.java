package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.constraints.NotNull;

public record ArbeidsforholdDto(@NotNull String organisasjonsnavn, @NotNull String organisasjonsnummer) {
}

