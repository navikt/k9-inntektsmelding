package no.nav.familie.inntektsmelding.integrasjoner.person;

import no.nav.familie.inntektsmelding.typer.AktørIdDto;

public record PersonInfo (String navn, PersonIdent fødselsnummer, AktørIdDto aktørId){}
