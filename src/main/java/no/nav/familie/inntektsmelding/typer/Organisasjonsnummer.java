package no.nav.familie.inntektsmelding.typer;

import java.util.Objects;

public record Organisasjonsnummer(String orgnr) {

    public Organisasjonsnummer {
        Objects.requireNonNull(orgnr, "orgnr");
        if (!OrganisasjonsNummerValidator.erGyldig(orgnr)) {
            throw new IllegalArgumentException("Orgnummer er ugyldig");
        }
    }

}
