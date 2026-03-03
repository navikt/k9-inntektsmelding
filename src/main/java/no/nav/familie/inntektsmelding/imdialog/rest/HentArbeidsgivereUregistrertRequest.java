package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record HentArbeidsgivereUregistrertRequest(@Valid @NotNull PersonIdent fødselsnummer,
                                                  @Valid @NotNull YtelseTypeDto ytelseType) {
}
