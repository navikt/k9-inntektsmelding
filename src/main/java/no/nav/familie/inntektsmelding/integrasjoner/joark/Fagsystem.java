package no.nav.familie.inntektsmelding.integrasjoner.joark;

public enum Fagsystem {

    FPSAK("FS36"),
    K9SAK("K9"),
    ;

    private String offisiellKode;

    Fagsystem() {
        // Hibernate trenger den
    }

    private Fagsystem(String offisiellKode) {
        this.offisiellKode = offisiellKode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }
}
