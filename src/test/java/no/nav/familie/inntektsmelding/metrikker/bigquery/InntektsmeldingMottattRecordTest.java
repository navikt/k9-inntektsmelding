package no.nav.familie.inntektsmelding.metrikker.bigquery;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingMottattRecordTest {

    private static final LocalDate START_DATO = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime OPPRETTET_TIDSPUNKT = LocalDateTime.of(2026, 1, 15, 12, 30);

    private static InntektsmeldingEntitet.Builder grunnlag() {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medArbeidsgiverIdent("999999999")
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medInntektsmeldingType(InntektsmeldingType.ORDINÆR)
            .medStartDato(START_DATO)
            .medMånedInntekt(BigDecimal.valueOf(40000))
            .medOpprettetTidspunkt(OPPRETTET_TIDSPUNKT);
    }

    @Test
    void skal_sette_alle_flagg_false_uten_refusjon_endring_eller_naturalytelse() {
        var im = grunnlag().build();

        var record = InntektsmeldingMottattRecord.fra(im);

        assertThat(record.uuid()).isEqualTo(im.getUuid());
        assertThat(record.opprettetTidspunkt()).isEqualTo(OPPRETTET_TIDSPUNKT);
        assertThat(record.ytelsetype()).isEqualTo(Ytelsetype.PLEIEPENGER_SYKT_BARN);
        assertThat(record.kildesystem()).isEqualTo(Kildesystem.ARBEIDSGIVERPORTAL);
        assertThat(record.inntektsmeldingType()).isEqualTo(InntektsmeldingType.ORDINÆR);
        assertThat(record.harEndretInntekt()).isFalse();
        assertThat(record.harOppgittRefusjon()).isFalse();
        assertThat(record.harOppgittEndringerIRefusjon()).isFalse();
        assertThat(record.harOppgittOpphørAvRefusjon()).isFalse();
        assertThat(record.harOppgittNaturalytelse()).isFalse();
    }

    @Test
    void skal_sette_alle_flagg_true() {
        var im = grunnlag()
            .medMånedRefusjon(BigDecimal.valueOf(40000))
            .medRefusjonOpphørsdato(START_DATO.plusMonths(2))
            .medRefusjonsendringer(List.of(new RefusjonsendringEntitet(START_DATO.plusMonths(1), BigDecimal.valueOf(20000))))
            .medEndringsårsaker(List.of(EndringsårsakEntitet.builder().medÅrsak(Endringsårsak.TARIFFENDRING).medFom(START_DATO).build()))
            .medBortfaltNaturalytelser(List.of(BortaltNaturalytelseEntitet.builder()
                .medPeriode(START_DATO, Tid.TIDENES_ENDE)
                .medMånedBeløp(BigDecimal.valueOf(1000))
                .medType(NaturalytelseType.BIL)
                .build()))
            .build();

        var record = InntektsmeldingMottattRecord.fra(im);

        assertThat(record.harEndretInntekt()).isTrue();
        assertThat(record.harOppgittRefusjon()).isTrue();
        assertThat(record.harOppgittEndringerIRefusjon()).isTrue();
        assertThat(record.harOppgittOpphørAvRefusjon()).isTrue();
        assertThat(record.harOppgittNaturalytelse()).isTrue();
    }

    @Test
    void skal_ikke_regne_refusjon_på_0_eller_opphør_på_tidenes_ende_som_oppgitt() {
        var im = grunnlag()
            .medMånedRefusjon(BigDecimal.ZERO)
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .build();

        var record = InntektsmeldingMottattRecord.fra(im);

        assertThat(record.harOppgittRefusjon()).isFalse();
        assertThat(record.harOppgittOpphørAvRefusjon()).isFalse();
    }

    @Test
    void radmapper_skal_gi_kolonner_som_matcher_schema() {
        var tabell = InntektsmeldingMottattTabell.INSTANCE;
        var record = InntektsmeldingMottattRecord.fra(grunnlag().build());

        var rad = tabell.getRowMapper(null).apply(record);

        var kolonner = tabell.getSchema().getFields().stream().map(f -> f.getName()).toList();
        assertThat(rad.keySet()).containsExactlyInAnyOrderElementsOf(kolonner);
        assertThat(rad.get(InntektsmeldingMottattTabell.OPPRETTET_TID)).isEqualTo("2026-01-15T11:30:00Z");
        assertThat(rad.get(InntektsmeldingMottattTabell.UUID)).isEqualTo(record.uuid().toString());
        assertThat(rad.get(InntektsmeldingMottattTabell.YTELSE_TYPE)).isEqualTo("PLEIEPENGER_SYKT_BARN");
        assertThat(rad.get(InntektsmeldingMottattTabell.HAR_OPPGITT_REFUSJON)).isEqualTo(false);
    }
}
