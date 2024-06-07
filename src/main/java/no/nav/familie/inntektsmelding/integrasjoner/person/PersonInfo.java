package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.time.LocalDate;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public record PersonInfo(String navn, PersonIdent fødselsnummer, AktørIdEntitet aktørId, LocalDate fødselsdato) {
}
