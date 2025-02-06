package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

class ForespørselTeksterTest {

    @Test
    void lagSaksTittel() {
        String saksTittel = ForespørselTekster.lagSaksTittel("OLA NORDMANN", LocalDate.of(2021, 02, 1));
        assertEquals("Inntektsmelding for Ola Nordmann (01.02.21)", saksTittel);
    }

    @Test
    void lagOppgaveTekst() {
        String oppgaveTekst = ForespørselTekster.lagOppgaveTekst(Ytelsetype.OPPLÆRINGSPENGER);
        assertEquals("Innsending av inntektsmelding for opplæringspenger", oppgaveTekst);
    }

    @Test
    void lagTilleggsInformasjon_EksternInnsending() {
        LocalDate førsteFraværsdag = LocalDate.now();
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.EKSTERN_INNSENDING, førsteFraværsdag);
        var forventetTekst = String.format("Utført i Altinn eller i bedriftens lønns- og personalsystem for første fraværsdag %s",
                                           førsteFraværsdag.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
        assertEquals(forventetTekst, statusTekst);
    }

    @Test
    void lagTilleggsInformasjon_OrdinærInnsending() {
        LocalDate førsteFraværsdag = LocalDate.now();
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, førsteFraværsdag);
        var forventetTekst = String.format("For første fraværsdag %s", førsteFraværsdag.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
        assertEquals(forventetTekst, statusTekst);
    }

    @Test
    void lagTilleggsInformasjon_Utgått() {
        LocalDate førsteFraværsdag = LocalDate.now();
        String statusTekst = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, førsteFraværsdag);
        var forventetTekst = String.format("Du trenger ikke lenger sende inntektsmelding for første fraværsdag %s",
                                           førsteFraværsdag.format(DateTimeFormatter.ofPattern("dd.MM.yy")));
        assertEquals(forventetTekst, statusTekst);
    }

    @Test
    void legVarselTekstMedOrgnvOgNavn() {
        var testOrgNavn = "test org";
        var testOrgNr = "1234321";
        var varselTekst = ForespørselTekster.lagVarselTekst(Ytelsetype.PLEIEPENGER_SYKT_BARN, new Organisasjon(testOrgNavn, testOrgNr));

        assertThat(varselTekst).isNotEmpty()
            .startsWith(testOrgNavn.toUpperCase())
            .contains(testOrgNr)
            .contains("pleiepenger sykt barn");
    }

    @Test
    void legPåminnelseTekstMedOrgnvOgNavn() {
        var testOrgNavn = "org test org";
        var testOrgNr = "6531342";
        var varselTekst = ForespørselTekster.lagPåminnelseTekst(Ytelsetype.PLEIEPENGER_NÆRSTÅENDE, new Organisasjon(testOrgNavn, testOrgNr));

        assertThat(varselTekst).isNotEmpty()
            .startsWith(testOrgNavn.toUpperCase())
            .contains(testOrgNr)
            .contains("pleiepenger i livets sluttfase");
    }
}
