package no.nav.familie.inntektsmelding.integrasjoner.person;

import java.time.LocalDate;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public record PersonInfo(String fornavn, String mellomnavn, String etternavn, PersonIdent fødselsnummer, AktørIdEntitet aktørId,
                         LocalDate fødselsdato, String telefonnummer) {

    public String mapNavn() {
        if (etternavn == null || fornavn == null) {
            return "";
        }
        return etternavn + " " + fornavn + (mellomnavn == null ? "" : " " + mellomnavn);
    }

    public String mapFulltNavn() {
        if (etternavn == null || fornavn == null) {
            return "";
        }
        return fornavn + (mellomnavn == null ? "" : " " + mellomnavn) +" "+ etternavn;
    }

    public String mapFornavn() {
        if (fornavn == null) {
            return "";
        }
        return fornavn;
    }
}
