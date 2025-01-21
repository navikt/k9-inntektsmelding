package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record SlåOppArbeidstakerResponseDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn, @NotNull @Valid Set<ArbeidsforholdDto> arbeidsforhold) {
    public record ArbeidsforholdDto(@NotNull String organisasjonsnavn, @NotNull String organisasjonsnummer) {}
}
