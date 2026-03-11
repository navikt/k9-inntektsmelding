package no.nav.familie.inntektsmelding.imdialog.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record OpplysningerRequestDto(@Valid @NotNull PersonIdent fødselsnummer,
                                     @Valid @NotNull YtelseTypeDto ytelseType,
                                     @Valid @NotNull LocalDate førsteFraværsdag,
                                     @Valid @NotNull OrganisasjonsnummerDto organisasjonsnummer) {
}
