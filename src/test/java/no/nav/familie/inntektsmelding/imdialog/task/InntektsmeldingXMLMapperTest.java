package no.nav.familie.inntektsmelding.imdialog.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBElement;

import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;
import no.nav.familie.inntektsmelding.koder.Kildesystem;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;
import no.seres.xsd.nav.inntektsmelding_m._20181211.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Omsorgspenger;

class InntektsmeldingXMLMapperTest {

    private static final LocalDate NOW = LocalDate.now();
    private static final String DUMMY_ARBEIDSGIVER_IDENT = "0123456789";
    private static final AktørIdEntitet DUMMY_AKTØRID = AktørIdEntitet.dummy();
    private static final String DUMMY_FNR = "12345678900";

    @Test
    void test_gjennoptatt_naturalytelse_ok() {
        var forventetNaturalytelseType = NaturalytelseType.BIL;
        var forventetBeløp = BigDecimal.valueOf(1000L);
        var forventetFom = NOW.plusDays(31);

        var aktøridFnrMap = Map.of(DUMMY_AKTØRID, PersonIdent.fra(DUMMY_FNR));

        var inntektsmelding = lagInntektsmeldingEntitet(DUMMY_AKTØRID,
            List.of(lagBortfaltNaturalytelseEntitet(NOW, forventetFom, forventetNaturalytelseType, forventetBeløp)), Kildesystem.ARBEIDSGIVERPORTAL);

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, aktøridFnrMap);

        var opphoerAvNaturalytelse = resultat.getSkjemainnhold().getOpphoerAvNaturalytelseListe().getValue().getOpphoerAvNaturalytelse();
        assertThat(opphoerAvNaturalytelse).hasSize(1);
        assertNaturalytelse(opphoerAvNaturalytelse.getFirst(), NOW, forventetNaturalytelseType, forventetBeløp);

        var gjennoptatteNaturalytelser = resultat.getSkjemainnhold().getGjenopptakelseNaturalytelseListe().getValue().getNaturalytelseDetaljer();
        assertThat(gjennoptatteNaturalytelser).hasSize(1);
        assertNaturalytelse(gjennoptatteNaturalytelser.getFirst(), forventetFom.plusDays(1), forventetNaturalytelseType, forventetBeløp);
    }

    @Test
    void test_bortfallt_naturalytelse_ok() {
        var forventetNaturalytelseType = NaturalytelseType.BIL;
        var forventetBeløp = BigDecimal.valueOf(1000L);
        var forventetFom = NOW;

        var aktøridFnrMap = Map.of(DUMMY_AKTØRID, PersonIdent.fra(DUMMY_FNR));

        var inntektsmelding = lagInntektsmeldingEntitet(DUMMY_AKTØRID,
            List.of(lagBortfaltNaturalytelseEntitet(forventetFom, Tid.TIDENES_ENDE, forventetNaturalytelseType, forventetBeløp)), Kildesystem.ARBEIDSGIVERPORTAL
        );

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, aktøridFnrMap);

        var opphoerAvNaturalytelse = resultat.getSkjemainnhold().getOpphoerAvNaturalytelseListe().getValue().getOpphoerAvNaturalytelse();
        assertThat(opphoerAvNaturalytelse).hasSize(1);
        assertNaturalytelse(opphoerAvNaturalytelse.getFirst(), forventetFom, forventetNaturalytelseType, forventetBeløp);

        assertThat(resultat.getSkjemainnhold().getGjenopptakelseNaturalytelseListe().getValue().getNaturalytelseDetaljer()).isEmpty();
    }

    @Test
    void test_omsorgspenger() {
        var forventetFraværsPeriodeFom = NOW;
        var forventetFraværsPeriodeTom = NOW.plusDays(1);
        var forventetFraværsPeriodeFom2 = NOW.plusWeeks(1);
        var forventetFraværsPeriodeTom2 = NOW.plusWeeks(1).plusDays(1);

        var forventetDelvisFraværDato = NOW;
        var forventetDelvisFraværDato2 = NOW.plusDays(1);
        var forventetTimer = BigDecimal.valueOf(3);
        var forventetTimer2 = BigDecimal.valueOf(4);

        var aktøridFnrMap = Map.of(DUMMY_AKTØRID, PersonIdent.fra(DUMMY_FNR));

        var omsorgspenger = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true)
            .medFraværsPerioder(List.of(new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(forventetFraværsPeriodeFom, forventetFraværsPeriodeTom)),
                new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(forventetFraværsPeriodeFom2, forventetFraværsPeriodeTom2))))
            .medDelvisFraværsPerioder(List.of(new DelvisFraværsPeriodeEntitet(forventetDelvisFraværDato, forventetTimer),
                new DelvisFraværsPeriodeEntitet(forventetDelvisFraværDato2, forventetTimer2)));


        var inntektsmelding = lagInntektsmeldingEntitet(DUMMY_AKTØRID, Kildesystem.ARBEIDSGIVERPORTAL, omsorgspenger.build());

        var resultat = InntektsmeldingXMLMapper.map(inntektsmelding, aktøridFnrMap);

        var omsorgspengerXML = resultat.getSkjemainnhold().getOmsorgspenger();
        assertThat(omsorgspengerXML.getValue()).isNotNull();
        assertFravær(omsorgspengerXML, forventetFraværsPeriodeFom, forventetFraværsPeriodeTom, forventetFraværsPeriodeFom2, forventetFraværsPeriodeTom2);
        assertDelvisFravær(omsorgspengerXML, forventetDelvisFraværDato, forventetTimer, forventetDelvisFraværDato2, forventetTimer2);
    }

    private static void assertFravær(JAXBElement<Omsorgspenger> omsorgspengerXML,
                                  LocalDate forventetFraværsPeriodeFom,
                                  LocalDate forventetFraværsPeriodeTom,
                                  LocalDate forventetFraværsPeriodeFom2,
                                  LocalDate forventetFraværsPeriodeTom2) {
        assertThat((omsorgspengerXML.getValue().getFravaersPerioder().getValue().getFravaerPeriode().getFirst().getFom().getValue())).isEqualTo(forventetFraværsPeriodeFom);
        assertThat((omsorgspengerXML.getValue().getFravaersPerioder().getValue().getFravaerPeriode().getFirst().getTom().getValue())).isEqualTo(forventetFraværsPeriodeTom);
        assertThat((omsorgspengerXML.getValue().getFravaersPerioder().getValue().getFravaerPeriode().getLast().getFom().getValue())).isEqualTo(forventetFraværsPeriodeFom2);
        assertThat((omsorgspengerXML.getValue().getFravaersPerioder().getValue().getFravaerPeriode().getLast().getTom().getValue())).isEqualTo(forventetFraværsPeriodeTom2);
    }

    private static void assertDelvisFravær(JAXBElement<Omsorgspenger> omsorgspengerXML,
                                  LocalDate forventetDelvisFraværDato,
                                  BigDecimal forventetTimer,
                                  LocalDate forventetDelvisFraværDato2,
                                  BigDecimal forventetTimer2) {
        assertThat(omsorgspengerXML.getValue().getDelvisFravaersListe().getValue().getDelvisFravaer().getFirst().getDato().getValue()).isEqualTo(forventetDelvisFraværDato);
        assertThat(omsorgspengerXML.getValue().getDelvisFravaersListe().getValue().getDelvisFravaer().getFirst().getTimer().getValue()).isEqualTo(forventetTimer);
        assertThat(omsorgspengerXML.getValue().getDelvisFravaersListe().getValue().getDelvisFravaer().getLast().getDato().getValue()).isEqualTo(forventetDelvisFraværDato2);
        assertThat(omsorgspengerXML.getValue().getDelvisFravaersListe().getValue().getDelvisFravaer().getLast().getTimer().getValue()).isEqualTo(forventetTimer2);
    }


    private static void assertNaturalytelse(NaturalytelseDetaljer naturalytelseDetaljer,
                                            LocalDate forventetFom,
                                            NaturalytelseType forventetNaturalytelseType,
                                            BigDecimal forventetBeløp) {
        assertThat(naturalytelseDetaljer.getFom().getValue()).isEqualTo(forventetFom);
        assertThat(naturalytelseDetaljer.getNaturalytelseType().getValue()).isEqualTo(forventetNaturalytelseType.name().toLowerCase());
        assertThat(naturalytelseDetaljer.getBeloepPrMnd().getValue()).isEqualTo(forventetBeløp);
    }

    private static InntektsmeldingEntitet lagInntektsmeldingEntitet(AktørIdEntitet aktørId,
                                                                    List<BortaltNaturalytelseEntitet> bortfaltNaturalytelseEntitet,
                                                                    Kildesystem kildesystem) {
        return InntektsmeldingEntitet.builder()
            .medBortfaltNaturalytelser(bortfaltNaturalytelseEntitet)
            .medArbeidsgiverIdent(DUMMY_ARBEIDSGIVER_IDENT)
            .medAktørId(aktørId)
            .medKildesystem(kildesystem)
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .build();
    }

    private static InntektsmeldingEntitet lagInntektsmeldingEntitet(AktørIdEntitet aktørId,
                                                                    Kildesystem kildesystem,
                                                                    OmsorgspengerEntitet omsorgspenger) {
        return InntektsmeldingEntitet.builder()
            .medArbeidsgiverIdent(DUMMY_ARBEIDSGIVER_IDENT)
            .medAktørId(aktørId)
            .medKildesystem(kildesystem)
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medOmsorgspenger(omsorgspenger)
            .build();
    }

    private static BortaltNaturalytelseEntitet lagBortfaltNaturalytelseEntitet(LocalDate fom,
                                                                               LocalDate tom,
                                                                               NaturalytelseType type,
                                                                               BigDecimal beløp) {
        return BortaltNaturalytelseEntitet.builder()
            .medPeriode(fom, tom)
            .medType(type)
            .medMånedBeløp(beløp)
            .build();
    }
}
