package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String TILLEGGSINFORMASJON_OMS = "For %s.";

    private static final Logger LOG = LoggerFactory.getLogger(ForespørselTekster.class);

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

    public static String lagTilleggsInformasjonForOmsorgspenger(List<FraværsPeriodeEntitet> fraværsPerioder,
                                                                List<DelvisFraværsPeriodeEntitet> delvisFraværDag) {
        List<LocalDate> fravær = sammenstillFravær(fraværsPerioder, delvisFraværDag);

        Map<Month, Long> fraværPerMåned = fravær
            .stream()
            .collect(Collectors.groupingBy(Month::from, Collectors.counting()));

        var tilleggsinfo = String.format(TILLEGGSINFORMASJON_OMS,
            fraværPerMåned
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(måned -> String.format("%s %s i %s", måned.getValue(), dagEllerDager(måned.getValue()), måned.getKey().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("NO"))))
                .collect(Collectors.joining(", ")));


        if (tilleggsinfo.length() >= 140) {
            LOG.info("Mottok tilleggsinfo med mange perioder: {}", tilleggsinfo);
            return tilleggsinfo.substring(0, 137) + "...";
        }

        return tilleggsinfo;
    }

    private static String dagEllerDager(long antallDager) {
        return antallDager == 1 ? "dag" : "dager";
    }

    private static List<LocalDate> sammenstillFravær(List<FraværsPeriodeEntitet> fraværsPerioder,
                                              List<DelvisFraværsPeriodeEntitet> delvisFraværDag) {
        List<LocalDate> fravær = new ArrayList<>();
        for (FraværsPeriodeEntitet fraværsPeriode : fraværsPerioder) {
            LocalDate fraværsDato = fraværsPeriode.getPeriode().getFom();
            while (fraværsDato.isBefore(fraværsPeriode.getPeriode().getTom()) || fraværsDato.isEqual(fraværsPeriode.getPeriode().getTom())) {
                fravær.add(fraværsDato);
                fraværsDato = fraværsDato.plusDays(1);
            }
        }
        fravær.addAll(delvisFraværDag
            .stream()
            .map(DelvisFraværsPeriodeEntitet::getDato)
            .toList());
        fravær.sort(Comparator.naturalOrder());
        return fravær;
    }
    public static String lagOppgaveTekst(Ytelsetype ytelseType) {
        return String.format(OPPGAVE_TEKST_NY, mapYtelsestypeNavn(ytelseType));
    }

    public static String lagSaksTittelInntektsmelding(String navn, LocalDate fødselsdato) {
        return String.format("Inntektsmelding for %s (%s)", capitalizeFully(navn), fødselsdato.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
    }

    public static String lagSaksTittelRefusjonskrav(String navn, LocalDate fødselsdato) {
        return String.format("Refusjonskrav for %s (%s)", capitalizeFully(navn), fødselsdato.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
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
