package no.nav.familie.inntektsmelding.rest.arbeidsgivernotifikasjon;

import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;

class ArbeidsgiverNotifikasjonUtils {

    public static Merkelapp getMerkelapp(Ytelsetype ytelse) {

        return switch (ytelse) {
            case FORELDREPENGER -> Merkelapp.INNTEKTSMELDING_FP;
            case SVANGERSKAPSPENGER -> Merkelapp.INNTEKTSMELDING_SVP;
            case PLEIEPENGER_SYKT_BARN -> Merkelapp.INNTEKTSMELDING_PSB;
            case PLEIEPENGER_I_LIVETS_SLUTTFASE -> Merkelapp.INNTEKTSMELDING_PILS;
            case OPPLÆRINGSPENGER -> Merkelapp.INNTEKTSMELDING_OPP;
            case OMSORGSPENGER -> Merkelapp.INNTEKTSMELDING_OMP;
        };
    }

    public static String getNavn(Ytelsetype ytelse) {
        return switch (ytelse) {
            case FORELDREPENGER -> "foreldrepenger";
            case SVANGERSKAPSPENGER -> "svangerskapspenger";
            case PLEIEPENGER_SYKT_BARN -> "pleiepenger sykt barn";
            case PLEIEPENGER_I_LIVETS_SLUTTFASE -> "pleiepenger i livets sluttfase";
            case OPPLÆRINGSPENGER -> "opplæringspenger";
            case OMSORGSPENGER -> "omsorgspenger";
        };
    }

    public static String getCapitalizedNavn(Ytelsetype ytelse) {
        return StringUtils.capitalize(getNavn(ytelse));
    }

    static String lagSaksTittel(Ytelsetype ytelse, String brukerID) {
        return String.format("Inntektsmelding %s for %s", brukerID, getCapitalizedNavn(ytelse));
    }

    static String lagNotifikasjonstekst(Ytelsetype ytelse) {
        return String.format("Inntektsmelding %s", getCapitalizedNavn(ytelse));
    }

    static URI lagNotifikasjonsLenke() {
        return URI.create("https://nav.no");
    }
}
