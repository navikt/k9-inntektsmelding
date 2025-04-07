package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;

import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

class ForespørselTeksterTest {

    @Test
    void lagSaksTittel() {
        String saksTittel = ForespørselTekster.lagSaksTittel("OLA NORDMANN", LocalDate.of(2021, 02, 1), Ytelsetype.PLEIEPENGER_SYKT_BARN);
        assertEquals("Inntektsmelding for Ola Nordmann (01.02.21)", saksTittel);
    }

    @Test
    void lagSaksTittelOmsorgspengerRefusjon() {
        String saksTittel = ForespørselTekster.lagSaksTittel("OLA NORDMANN", LocalDate.of(2021, 02, 1), Ytelsetype.OMSORGSPENGER);
        assertEquals("Refusjonskrav for Ola Nordmann (01.02.21)", saksTittel);
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
    void lagTilleggsInformasjon_OmsorgspengerRefusjon() {
        List<FraværsPeriodeEntitet> fraværsPerioder = List.of(
            new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 3, 25), LocalDate.of(2025, 3, 27))),
            new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 3, 29), LocalDate.of(2025, 3, 31))));
        List<DelvisFraværsPeriodeEntitet> delvisFravær = List.of(
            new DelvisFraværsPeriodeEntitet(LocalDate.of(2025, 3, 23), BigDecimal.valueOf(2)),
            new DelvisFraværsPeriodeEntitet(LocalDate.of(2025, 4, 2), BigDecimal.valueOf(4)));
        String statusTekst = ForespørselTekster.lagTilleggsInformasjonForOmsorgspengerRefusjon(fraværsPerioder, delvisFravær);
        var forventetTekst = "For 7 dager i mars, 1 dag i april.";
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
