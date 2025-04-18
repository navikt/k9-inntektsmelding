package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import java.time.LocalDate;

public record OpplysningerRequestDto(@Valid @NotNull PersonIdent fødselsnummer,
                                     @Valid @NotNull Ytelsetype ytelseType,
                                     @Valid @NotNull LocalDate førsteFraværsdag,
                                     @Valid @NotNull OrganisasjonsnummerDto organisasjonsnummer) {
}
