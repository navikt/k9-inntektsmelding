package no.nav.familie.inntektsmelding.typer;

import java.util.Objects;

public final class Organisasjonsnummer {

    private String orgnr;


    public Organisasjonsnummer() {
    }

    public Organisasjonsnummer(String orgnr) {
        Objects.requireNonNull(orgnr, "orgnr");
        if (!OrganisasjonsNummerValidator.erGyldig(orgnr)) {
            throw new IllegalArgumentException("Orgnummer er ugyldig");
        }
        this.orgnr = orgnr;
    }

    public String getOrgnr() {
        return orgnr;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Organisasjonsnummer) obj;
        return Objects.equals(this.orgnr, that.orgnr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgnr);
    }

    @Override
    public String toString() {
        return "Organisasjonsnummer[" + "orgnr=" + orgnr.substring(0, Math.min(orgnr.length(), 3)) + "...]";
    }


}
