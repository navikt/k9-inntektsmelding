package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public record SlåOppArbeidstakerDto(@Valid @NotNull PersonIdent fødselsnummer, @Valid @NotNull Ytelsetype ytelseType) {
    public SlåOppArbeidstakerDto {
    }
}

