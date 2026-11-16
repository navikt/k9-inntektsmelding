package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;

public record HentInntektsmeldingerForÅrV2Request(@NotNull @Valid PersonIdent fødselsnummer,
                                                  @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                                  @NotNull Integer år) {
}
