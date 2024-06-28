package no.nav.familie.inntektsmelding.integrasjoner.person;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import java.time.LocalDate;

public record PersonInfo(String fornavn, String mellomnavn, String etternavn, PersonIdent fødselsnummer,
                         AktørIdEntitet aktørId, LocalDate fødselsdato, String telefonnummer) {

    public String mapNavn() {
        if (etternavn == null || fornavn == null) {
            return "";
        }
        return etternavn + " " + fornavn + (mellomnavn == null ? "" : " " + mellomnavn);
    }
}
