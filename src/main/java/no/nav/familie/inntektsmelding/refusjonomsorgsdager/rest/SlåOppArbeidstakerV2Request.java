package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public record SlåOppArbeidstakerV2Request(@Valid @NotNull PersonIdent fødselsnummer,
                                          @Valid @NotNull Ytelsetype ytelseType,
                                          Integer årstall,
                                          @NotNull @Pattern(regexp = "^\\d{9}$") String organisasjonsnummer) {
}
