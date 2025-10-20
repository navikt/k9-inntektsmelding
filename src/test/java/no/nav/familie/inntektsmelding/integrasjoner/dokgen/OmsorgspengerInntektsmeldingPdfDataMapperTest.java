package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.familie.inntektsmelding.utils.FormatUtils;

@ExtendWith(MockitoExtension.class)
class OmsorgspengerInntektsmeldingPdfDataMapperTest {
    private static final String FORNAVN = "Test";
    private static final String MELLOMNAVN = "Tester";
    private static final String ETTERNAVN = "Testesen";
    private static final String ARBEIDSGIVER_IDENT = "999999999";
    private static final String ARBEIDSGIVER_NAVN = "Arbeidsgvier 1";
    private static final String NAVN = "Kontaktperson navn";
    private static final String ORG_NUMMER = "999999999";
    private static final BigDecimal REFUSJON_BELØP = BigDecimal.valueOf(35000);
    private static final AktørIdEntitet AKTØRID_SØKER = new AktørIdEntitet("1234567891234");
    private static final BigDecimal INNTEKT = BigDecimal.valueOf(40000);
    private static final LocalDateTime OPPRETTETT_TIDSPUNKT = LocalDateTime.now();
    private static final LocalDate START_DATO = LocalDate.now().minusMonths(1);
    private PersonInfo personInfo;
    private PersonIdent personIdent;

    @BeforeEach
    public void setUP() {
        personIdent = new PersonIdent("11111111111");
        personInfo = new PersonInfo(FORNAVN, MELLOMNAVN, ETTERNAVN, personIdent, AKTØRID_SØKER, LocalDate.now(), null, null);
    }

    @Test
    void skal_opprette_pdfData() {
        var fraværsPeriode = new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(START_DATO.plusDays(1), START_DATO.plusDays(3)));

        var omsorgspengerEntitet = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true)
            .medFraværsPerioder(List.of(fraværsPeriode))
            .build();

        var inntektsmeldingEntitet = lagStandardInntektsmeldingBuilder()
            .medOmsorgspenger(omsorgspengerEntitet)
            .medMånedRefusjon(REFUSJON_BELØP)
            .build();

        var pdfData = OmsorgspengerInntektsmeldingPdfDataMapper.map(inntektsmeldingEntitet, ARBEIDSGIVER_NAVN, personInfo, ARBEIDSGIVER_IDENT);

        assertThat(pdfData.arbeidsgiverIdent()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(pdfData.avsenderSystem()).isEqualTo("NAV_NO");
        assertThat(pdfData.arbeidsgiverNavn()).isEqualTo(ARBEIDSGIVER_NAVN);
        assertThat(pdfData.kontaktperson().navn()).isEqualTo(NAVN);
        assertThat(pdfData.kontaktperson().telefonnummer()).isEqualTo(ORG_NUMMER);
        assertThat(pdfData.månedInntekt()).isEqualTo(INNTEKT);
        assertThat(pdfData.navnSøker()).isEqualTo(FORNAVN + " " + MELLOMNAVN + " " + ETTERNAVN);
        assertThat(pdfData.opprettetTidspunkt()).isEqualTo(FormatUtils.formaterDatoOgTidNorsk(OPPRETTETT_TIDSPUNKT));
        assertThat(pdfData.personnummer()).isEqualTo(FormatUtils.formaterPersonnummer(personIdent.getIdent()));
        assertThat(pdfData.endringsarsaker()).isEmpty();

        assertThat(pdfData.harUtbetaltLønn()).isEqualTo("Ja");
        assertThat(pdfData.fraværsperioder()).hasSize(1);
        assertThat(pdfData.fraværsperioder().get(0).fom()).isEqualTo(FormatUtils.formaterDatoForLister(fraværsPeriode.getPeriode().getFom()));
        assertThat(pdfData.fraværsperioder().get(0).tom()).isEqualTo(FormatUtils.formaterDatoForLister(fraværsPeriode.getPeriode().getTom()));
    }

    @Test
    void skal_opprette_med_endringsårsaker() {
        var fraværsPeriode1 = new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(START_DATO.plusDays(1), START_DATO.plusDays(3)));
        var fraværsPeriode2 = new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(START_DATO.plusDays(7), START_DATO.plusDays(8)));

        var omsorgspengerEntitet = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true)
            .medFraværsPerioder(List.of(fraværsPeriode1, fraværsPeriode2))
            .build();

        var endringsårsakFom = LocalDate.of(2024, 6, 1);
        var endringsårsakTom = LocalDate.of(2024, 6, 30);
        var endringsårsakBleKjentFra = LocalDate.of(2024, 6, 30);

        List<EndringsårsakEntitet> endringsårsaker = List.of(EndringsårsakEntitet.builder()
                .medÅrsak(Endringsårsak.PERMISJON)
                .medFom(endringsårsakFom)
                .medTom(endringsårsakTom)
                .build(),
            EndringsårsakEntitet.builder()
                .medÅrsak(Endringsårsak.BONUS)
                .build(),
            EndringsårsakEntitet.builder()
                .medÅrsak(Endringsårsak.TARIFFENDRING)
                .medFom(endringsårsakFom)
                .medBleKjentFra(endringsårsakBleKjentFra)
                .build()
        );

        var inntektsmeldingEntitet = lagStandardInntektsmeldingBuilder()
            .medEndringsårsaker(endringsårsaker)
            .medOmsorgspenger(omsorgspengerEntitet)
            .build();

        var pdfData = OmsorgspengerInntektsmeldingPdfDataMapper.map(inntektsmeldingEntitet, ARBEIDSGIVER_NAVN, personInfo, ARBEIDSGIVER_IDENT);

        assertThat(pdfData.arbeidsgiverIdent()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(pdfData.avsenderSystem()).isEqualTo("NAV_NO");
        assertThat(pdfData.arbeidsgiverNavn()).isEqualTo(ARBEIDSGIVER_NAVN);
        assertThat(pdfData.kontaktperson().navn()).isEqualTo(NAVN);
        assertThat(pdfData.kontaktperson().telefonnummer()).isEqualTo(ORG_NUMMER);
        assertThat(pdfData.månedInntekt()).isEqualTo(INNTEKT);
        assertThat(pdfData.navnSøker()).isEqualTo(FORNAVN + " " + MELLOMNAVN + " " + ETTERNAVN);
        assertThat(pdfData.opprettetTidspunkt()).isEqualTo(FormatUtils.formaterDatoOgTidNorsk(OPPRETTETT_TIDSPUNKT));
        assertThat(pdfData.personnummer()).isEqualTo(FormatUtils.formaterPersonnummer(personIdent.getIdent()));

        assertThat(pdfData.harUtbetaltLønn()).isEqualTo("Nei");
        assertThat(pdfData.fraværsperioder()).hasSize(2);
        assertThat(pdfData.fraværsperioder().get(0).fom()).isEqualTo(FormatUtils.formaterDatoForLister(fraværsPeriode1.getPeriode().getFom()));
        assertThat(pdfData.fraværsperioder().get(0).tom()).isEqualTo(FormatUtils.formaterDatoForLister(fraværsPeriode1.getPeriode().getTom()));
        assertThat(pdfData.fraværsperioder().get(1).fom()).isEqualTo(FormatUtils.formaterDatoForLister(fraværsPeriode2.getPeriode().getFom()));
        assertThat(pdfData.fraværsperioder().get(1).tom()).isEqualTo(FormatUtils.formaterDatoForLister(fraværsPeriode2.getPeriode().getTom()));

        assertThat(pdfData.endringsarsaker().isEmpty()).isFalse();
        assertThat(pdfData.endringsarsaker()).hasSize(3);

        assertThat(pdfData.endringsarsaker().get(0).arsak()).isEqualTo(Endringsårsak.PERMISJON.getBeskrivelse());
        assertThat(pdfData.endringsarsaker().get(0).fom()).isEqualTo(FormatUtils.formaterDatoForLister(endringsårsakFom));
        assertThat(pdfData.endringsarsaker().get(0).tom()).isEqualTo(FormatUtils.formaterDatoForLister(endringsårsakTom));
        assertThat(pdfData.endringsarsaker().get(0).bleKjentFra()).isNull();

        assertThat(pdfData.endringsarsaker().get(1).arsak()).isEqualTo(Endringsårsak.BONUS.getBeskrivelse());
        assertThat(pdfData.endringsarsaker().get(1).fom()).isNull();
        assertThat(pdfData.endringsarsaker().get(1).tom()).isNull();
        assertThat(pdfData.endringsarsaker().get(1).bleKjentFra()).isNull();

        assertThat(pdfData.endringsarsaker().get(2).arsak()).isEqualTo(Endringsårsak.TARIFFENDRING.getBeskrivelse());
        assertThat(pdfData.endringsarsaker().get(2).fom()).isEqualTo(FormatUtils.formaterDatoForLister(endringsårsakFom));
        assertThat(pdfData.endringsarsaker().get(2).tom()).isNull();
        assertThat(pdfData.endringsarsaker().get(2).bleKjentFra()).isEqualTo(FormatUtils.formaterDatoForLister(endringsårsakBleKjentFra));
    }

    private InntektsmeldingEntitet.Builder lagStandardInntektsmeldingBuilder() {
        return InntektsmeldingEntitet.builder()
            .medAktørId(AKTØRID_SØKER)
            .medKontaktperson(new KontaktpersonEntitet(NAVN, ORG_NUMMER))
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medInntektsmeldingType(InntektsmeldingType.OMSORGSPENGER_REFUSJON)
            .medMånedInntekt(INNTEKT)
            .medStartDato(START_DATO)
            .medOpprettetTidspunkt(OPPRETTETT_TIDSPUNKT)
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT);
    }
}
