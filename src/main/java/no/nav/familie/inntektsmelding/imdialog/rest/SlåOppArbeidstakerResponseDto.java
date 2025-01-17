package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SlåOppArbeidstakerResponseDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn, @NotNull @Valid List<ArbeidsforholdDto> arbeidsforhold) {
    public record ArbeidsforholdDto(@NotNull String organisasjonsnavn, @NotNull String organisasjonsnummer) {}
}
