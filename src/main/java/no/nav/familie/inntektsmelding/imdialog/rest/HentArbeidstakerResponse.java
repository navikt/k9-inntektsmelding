package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.Kjønn;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public record HentArbeidstakerResponse(@NotNull String fornavn,
                                       String mellomnavn,
                                       @NotNull String etternavn,
                                       @NotNull Kjønn kjønn,
                                       @NotNull String aktørId
) {
}

