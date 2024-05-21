package no.nav.familie.inntektsmelding.integrasjoner.person;

import no.nav.familie.inntektsmelding.typer.AktørId;

public record PersonInfo (String navn, PersonIdent fødselsnummer, AktørId aktørId){}
