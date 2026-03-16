package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public enum InntektsFormål {
    FORMÅL_OMSORGSPENGER("Omsorgspenger"),
    FORMÅL_PLEIEPENGER_SYKT_BARN("PleiepengerSyktBarn"),
    FORMÅL_PLEIEPENGER_NÆRSTÅENDE("PleiepengerNaerstaaende"),
    FORMÅL_OPPLÆRINGSPENGER("Opplaeringspenger");

    private String kode;

    private InntektsFormål(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public static String utledInntektsFormål(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case PLEIEPENGER_SYKT_BARN -> FORMÅL_PLEIEPENGER_SYKT_BARN.getKode();
            case PLEIEPENGER_NÆRSTÅENDE -> FORMÅL_PLEIEPENGER_NÆRSTÅENDE.getKode();
            case OPPLÆRINGSPENGER -> FORMÅL_OPPLÆRINGSPENGER.getKode();
            case OMSORGSPENGER -> FORMÅL_OMSORGSPENGER.getKode();
        };
    }
}
