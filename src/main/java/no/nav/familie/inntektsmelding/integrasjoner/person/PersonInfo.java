package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.time.LocalDate;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;

public record PersonInfo(String navn, PersonIdent fødselsnummer, AktørIdDto aktørId, LocalDate fødselsdato) {
}
