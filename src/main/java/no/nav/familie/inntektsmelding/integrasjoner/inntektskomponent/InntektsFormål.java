package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public enum InntektsFormål {
    FORMAAL_OMSORSGPENGER("Omsorgspenger"),
    FORMAAL_PLEIEPENGER_SYKT_BARN("PleiepengerSyktBarn"),
    FORMAAL_PLEIEPENGER_NÆRSTÅENDE("PleiepengerNaerstaaende"),
    FORMAAL_OPPLÆRINGSPENGER("Opplaeringspenger");

    private String kode;

    private InntektsFormål(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public static String utledInntektsFormål(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case PLEIEPENGER_SYKT_BARN -> FORMAAL_PLEIEPENGER_SYKT_BARN.getKode();
            case PLEIEPENGER_NÆRSTÅENDE -> FORMAAL_PLEIEPENGER_NÆRSTÅENDE.getKode();
            case OPPLÆRINGSPENGER -> FORMAAL_OPPLÆRINGSPENGER.getKode();
            case OMSORGSPENGER -> FORMAAL_OMSORSGPENGER.getKode();
        };
    }
}
