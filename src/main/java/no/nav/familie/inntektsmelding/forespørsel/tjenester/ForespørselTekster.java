package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

class ForespørselTekster {
    private static final String OPPGAVE_TEKST_NY = "Send inntektsmelding for %s som har søkt om %s";


    public static final String STATUS_TEKST_DEFAULT = "";
    public static final String STATUS_TEKST_UTFØRT_EKSTERN = "Utført i  Altinn eller i bedriftens personalsystem";
    public static final String STATUS_TEKST_UTGÅTT = "Saksbehandler har gått videre uten inntektsmelding";


    public static String lagOppgaveTekst(String navn, Ytelsetype ytelseType) {
        return String.format(OPPGAVE_TEKST_NY, capitalizeFully(navn), mapYtelsestypeNavn(ytelseType));
    }

    public static String lagSaksTittel(String navn, LocalDate fødselsdato) {
        return String.format("Inntektsmelding for %s (%s)", capitalizeFully(navn),
                fødselsdato.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
    }

    static String capitalizeFully(String input) {
        return Arrays.stream(input.toLowerCase().split("\\s+"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }

    public static Merkelapp finnMerkelapp(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> Merkelapp.INNTEKTSMELDING_FP;
            case PLEIEPENGER_SYKT_BARN -> Merkelapp.INNTEKTSMELDING_PSB;
            case OMSORGSPENGER -> Merkelapp.INNTEKTSMELDING_OMP;
            case SVANGERSKAPSPENGER -> Merkelapp.INNTEKTSMELDING_SVP;
            case PLEIEPENGER_NÆRSTÅENDE -> Merkelapp.INNTEKTSMELDING_PILS;
            case OPPLÆRINGSPENGER -> Merkelapp.INNTEKTSMELDING_OPP;
        };
    }
    public  static String mapYtelsestypeNavn(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> "foreldrepenger";
            case PLEIEPENGER_SYKT_BARN -> "pleiepenger for sykt barn" ;
            case OMSORGSPENGER -> "omsorgspenger";
            case SVANGERSKAPSPENGER -> "svangerskapspenger";
            case PLEIEPENGER_NÆRSTÅENDE -> "pleiepenger for nærtstående";
            case OPPLÆRINGSPENGER -> "opplæringspenger";
        };
    }
}
