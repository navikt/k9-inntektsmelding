package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;

public record HentInntektsmeldingerForÅrRequest(@NotNull @Valid AktørIdDto aktørId,
                                                @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                                @NotNull Integer år) {
}
