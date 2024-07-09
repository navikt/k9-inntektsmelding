package no.nav.familie.inntektsmelding.integrasjoner.dokgen;


import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoNorsk;
import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoOgTidNorsk;
import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterPersonnummer;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.NaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonPeriodeEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingPdfDataMapperTest {

    @Test
    public void skal_opprette_pdfData() {
        var aktørIdSøker = new AktørIdEntitet("1234567891234");
        var refusjonsbeløp = BigDecimal.valueOf(35000);
        var refusjonperiode = new RefusjonPeriodeEntitet(LocalDate.of(2024, 6, 1), Tid.TIDENES_ENDE, refusjonsbeløp);
        var naturalytelseFraDato = LocalDate.of(2024, 6, 10);
        var naturalytelseTilDato = LocalDate.of(2024, 6, 30);
        var naturalytelseBeløp = BigDecimal.valueOf(2000);
        var naturalytelse = NaturalytelseEntitet.builder()
            .medPeriode(naturalytelseFraDato, naturalytelseTilDato)
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medErBortfalt(true)
            .medBeløp(naturalytelseBeløp)
            .build();
        var arbeidsgiverIdent = "999999999";
        var arbeidsgiverNavn = "Arbeidsgvier 1";
        var navn = "Kontaktperson navn";
        var nr = "999999999";
        var inntekt = BigDecimal.valueOf(40000);

        var opprettetTidspunkt = LocalDateTime.now();
        var startdato = LocalDate.now();

        var inntektsmeldingEntitet = InntektsmeldingEntitet.builder()
            .medAktørId(aktørIdSøker)
            .medKontaktperson(new KontaktpersonEntitet(navn, nr))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(inntekt)
            .medStartDato(startdato)
            .medOpprettetTidspunkt(opprettetTidspunkt)
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medRefusjonsPeriode(List.of(refusjonperiode))
            .medNaturalYtelse(List.of(naturalytelse))
            .build();

        var personIdent = new PersonIdent("11111111111");

        var personinfo = new PersonInfo("Test", "Tester", "Testesen", personIdent, inntektsmeldingEntitet.getAktørId(), LocalDate.now(), null);

        var pdfData = InntektsmeldingPdfDataMapper.mapInntektsmeldingData(inntektsmeldingEntitet, arbeidsgiverNavn, personinfo, arbeidsgiverIdent);


        assertThat(pdfData.getArbeidsgiverIdent()).isEqualTo(arbeidsgiverIdent);
        assertThat(pdfData.getAvsenderSystem()).isEqualTo("NAV_NO");
        assertThat(pdfData.getArbeidsgiverNavn()).isEqualTo(arbeidsgiverNavn);
        assertThat(pdfData.getKontaktperson().navn()).isEqualTo(navn);
        assertThat(pdfData.getKontaktperson().telefonnummer()).isEqualTo(nr);
        assertThat(pdfData.getMånedInntekt()).isEqualTo(inntekt);
        assertThat(pdfData.getNavnSøker()).isEqualTo("Testesen Test Tester");
        assertThat(pdfData.getYtelsetype()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(pdfData.getOpprettetTidspunkt()).isEqualTo(formaterDatoOgTidNorsk(opprettetTidspunkt));
        assertThat(pdfData.getStartDato()).isEqualTo(formaterDatoNorsk(startdato));
        assertThat(pdfData.getPersonnummer()).isEqualTo(formaterPersonnummer(personIdent.getIdent()));
        assertThat(pdfData.getRefusjonOpphørsdato()).isNull();
        assertThat(pdfData.getRefusjonsbeløp()).isEqualTo(refusjonsbeløp);
        assertThat(pdfData.getEndringIrefusjonsperioder()).isEmpty();
        assertThat(pdfData.ingenGjenopptattNaturalytelse()).isTrue();
        assertThat(pdfData.ingenBortfaltNaturalytelse()).isFalse();
        assertThat(pdfData.getNaturalytelser().getFirst().fom()).isEqualTo(formaterDatoNorsk(naturalytelseFraDato));
        assertThat(pdfData.getNaturalytelser().getFirst().tom()).isEqualTo(formaterDatoNorsk(naturalytelseTilDato));
        assertThat(pdfData.getNaturalytelser().getFirst().beloep()).isEqualTo(naturalytelseBeløp);
        assertThat(pdfData.getNaturalytelser().getFirst().naturalytelseType()).isEqualTo("Aksjer grunnfondsbevis til underkurs");

    }
}
