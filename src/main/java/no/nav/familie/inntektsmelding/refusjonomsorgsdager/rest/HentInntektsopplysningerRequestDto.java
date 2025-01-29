package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

public record HentInntektsopplysningerRequestDto(
    @NotNull @Valid PersonIdent fødselsnummer,
    @NotNull @Pattern(regexp = "^\\d{9}$") @Valid String organisasjonsnummer,
    @NotNull String skjæringstidspunkt
) {
}
