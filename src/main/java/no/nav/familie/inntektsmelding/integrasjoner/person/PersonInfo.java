package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.time.LocalDate;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.pdl.Navn;

public record PersonInfo(Navn navn, PersonIdent fødselsnummer, AktørIdEntitet aktørId, LocalDate fødselsdato) {

    public String mapNavn() {
        if (navn.getEtternavn() == null || navn.getFornavn() == null) {
            return "";
        }
        return navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }
}
