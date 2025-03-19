package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

class ForespørselTekster {
    private static final String OPPGAVE_TEKST_NY = "Innsending av inntektsmelding for %s";
    private static final String VARSEL_TEKST = "%s - orgnr %s: En av dine ansatte har søkt om %s og vi trenger inntektsmelding for å behandle søknaden. Logg inn på Min side – arbeidsgiver hos Nav. Hvis dere sender inn via lønnssystem kan dere fortsette med dette.";
    private static final String BESKJED_FRA_SAKSBEHANDLER_TEKST = "Vi har ennå ikke mottatt inntektsmelding for %s. For at vi skal kunne behandle søknaden om %s, må inntektsmeldingen sendes inn så raskt som mulig.";
    private static final String VARSEL_FRA_SAKSBEHANDLER_TEKST = "%s - orgnr %s: Vi har ennå ikke mottatt inntektsmelding. For at vi skal kunne behandle søknaden om %s, må inntektsmeldingen sendes inn så raskt som mulig.";

    private static final String TILLEGGSINFORMASJON_UTFØRT_EKSTERN = "Utført i Altinn eller i bedriftens lønns- og personalsystem for første fraværsdag %s";
    private static final String TILLEGGSINFORMASJON_UTGÅTT = "Du trenger ikke lenger sende inntektsmelding for første fraværsdag %s";
    private static final String TILLEGGSINFORMASJON_ORDINÆR = "For første fraværsdag %s";

    private ForespørselTekster() {
        // Skjuler default
    }

    public static String lagTilleggsInformasjonOrdinær(LocalDate førsteFraværsdag) {
        return String.format(TILLEGGSINFORMASJON_ORDINÆR, førsteFraværsdag.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
    }

    public static String lagTilleggsInformasjon(LukkeÅrsak årsak, LocalDate førsteFraværsdag) {
        return switch (årsak) {
            case EKSTERN_INNSENDING -> String.format(TILLEGGSINFORMASJON_UTFØRT_EKSTERN, førsteFraværsdag.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
            case UTGÅTT -> String.format(TILLEGGSINFORMASJON_UTGÅTT, førsteFraværsdag.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
            case ORDINÆR_INNSENDING -> lagTilleggsInformasjonOrdinær(førsteFraværsdag);
        };
    }

    public static String lagOppgaveTekst(Ytelsetype ytelseType) {
        return String.format(OPPGAVE_TEKST_NY, mapYtelsestypeNavn(ytelseType));
    }

    public static String lagSaksTittel(String navn, LocalDate fødselsdato, Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OPPLÆRINGSPENGER -> String.format("Inntektsmelding for %s (%s)", capitalizeFully(navn), fødselsdato.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
            case OMSORGSPENGER -> String.format("Refusjonskrav for %s (%s)", capitalizeFully(navn), fødselsdato.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
        };
    }

    public static String lagVarselTekst(Ytelsetype ytelsetype, Organisasjon org) {
        return String.format(VARSEL_TEKST, org.navn().toUpperCase(), org.orgnr(), mapYtelsestypeNavn(ytelsetype));
    }

    public static String lagPåminnelseTekst(Ytelsetype ytelsetype, Organisasjon org) {
        return String.format(VARSEL_TEKST, org.navn().toUpperCase(), org.orgnr(), mapYtelsestypeNavn(ytelsetype));
    }

    static String capitalizeFully(String input) {
        return Arrays.stream(input.toLowerCase().split("\\s+")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    public static Merkelapp finnMerkelapp(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case PLEIEPENGER_SYKT_BARN -> Merkelapp.INNTEKTSMELDING_PSB;
            case OMSORGSPENGER -> Merkelapp.INNTEKTSMELDING_OMP;
            case PLEIEPENGER_NÆRSTÅENDE -> Merkelapp.INNTEKTSMELDING_PILS;
            case OPPLÆRINGSPENGER -> Merkelapp.INNTEKTSMELDING_OPP;
        };
    }

    public static String mapYtelsestypeNavn(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case PLEIEPENGER_SYKT_BARN -> "pleiepenger sykt barn";
            case OMSORGSPENGER -> "omsorgspenger";
            case PLEIEPENGER_NÆRSTÅENDE -> "pleiepenger i livets sluttfase";
            case OPPLÆRINGSPENGER -> "opplæringspenger";
        };
    }

    public static String lagBeskjedFraSaksbehandlerTekst(Ytelsetype ytelseType, String søkerNavn) {
        return String.format(BESKJED_FRA_SAKSBEHANDLER_TEKST, søkerNavn, mapYtelsestypeNavn(ytelseType));
    }

    public static String lagVarselFraSaksbehandlerTekst(Ytelsetype ytelsetype, Organisasjon org) {
        return String.format(VARSEL_FRA_SAKSBEHANDLER_TEKST, org.navn().toUpperCase(), org.orgnr(), mapYtelsestypeNavn(ytelsetype));
    }

}
