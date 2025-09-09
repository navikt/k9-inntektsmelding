package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.Kjønn;

public record HentArbeidsforholdResponse(@NotNull String fornavn,
                                         String mellomnavn,
                                         @NotNull String etternavn,
                                         @NotNull Kjønn kjønn,
                                         @NotNull @Valid Set<ArbeidsforholdDto> arbeidsforhold) {

    public record ArbeidsforholdDto(@NotNull String organisasjonsnavn, @NotNull String organisasjonsnummer) {}
}
