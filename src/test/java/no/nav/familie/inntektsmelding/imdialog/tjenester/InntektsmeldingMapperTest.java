package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingMapperTest {

    @Test
    void skal_teste_mapping_uten_ref_og_naturalytelse() {
        // Arrange
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID().toString(), new AktørIdDto("9999999999999"), YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"), new SendInntektsmeldingRequestDto.KontaktpersonDto("Testy test", "999999999"), LocalDate.now(),
            BigDecimal.valueOf(5000), Collections.emptyList(), Collections.emptyList());

        // Act
        var entitet = InntektsmeldingMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(entitet.getStartDato()).isEqualTo(request.startdato());
        assertThat(entitet.getYtelsetype()).isEqualTo(KodeverkMapper.mapYtelsetypeTilEntitet(request.ytelse()));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo(request.kontaktperson().navn());
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());
        assertThat(entitet.getNaturalYtelse()).isEmpty();
        assertThat(entitet.getRefusjonsPeriode()).isEmpty();
    }

    @Test
    void skal_teste_mapping_med_ref_og_naturalytelse() {
        // Arrange
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID().toString(), new AktørIdDto("9999999999999"), YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"), new SendInntektsmeldingRequestDto.KontaktpersonDto("Testy test", "999999999"), LocalDate.now(),
            BigDecimal.valueOf(5000), Collections.singletonList(new SendInntektsmeldingRequestDto.RefusjonsperiodeRequestDto(LocalDate.now(),
            Tid.TIDENES_ENDE, BigDecimal.valueOf(4000))), Collections.singletonList(new SendInntektsmeldingRequestDto.NaturalytelseRequestDto(LocalDate.now(),
            Tid.TIDENES_ENDE, NaturalytelsetypeDto.ANNET, true, BigDecimal.valueOf(4000))));

        // Act
        var entitet = InntektsmeldingMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(entitet.getStartDato()).isEqualTo(request.startdato());
        assertThat(entitet.getYtelsetype()).isEqualTo(KodeverkMapper.mapYtelsetypeTilEntitet(request.ytelse()));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo(request.kontaktperson().navn());
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());

        assertThat(entitet.getNaturalYtelse()).hasSize(1);
        assertThat(entitet.getNaturalYtelse().getFirst().getBeløp()).isEqualByComparingTo(request.bortfaltNaturaltytelsePerioder().getFirst().beløp());
        assertThat(entitet.getNaturalYtelse().getFirst().getType()).isEqualByComparingTo(KodeverkMapper.mapNaturalytelseTilEntitet(request.bortfaltNaturaltytelsePerioder().getFirst().naturalytelsetype()));
        assertThat(entitet.getNaturalYtelse().getFirst().getPeriode().getFom()).isEqualTo(request.bortfaltNaturaltytelsePerioder().getFirst().fom());
        assertThat(entitet.getNaturalYtelse().getFirst().getPeriode().getTom()).isEqualTo(request.bortfaltNaturaltytelsePerioder().getFirst().tom());
        assertThat(entitet.getNaturalYtelse().getFirst().getErBortfalt()).isEqualTo(request.bortfaltNaturaltytelsePerioder().getFirst().erBortfalt());



        assertThat(entitet.getRefusjonsPeriode()).hasSize(1);
        assertThat(entitet.getRefusjonsPeriode().getFirst().getBeløp()).isEqualByComparingTo(request.refusjonsperioder().getFirst().beløp());
        assertThat(entitet.getRefusjonsPeriode().getFirst().getPeriode().getFom()).isEqualTo(request.refusjonsperioder().getFirst().fom());
        assertThat(entitet.getRefusjonsPeriode().getFirst().getPeriode().getTom()).isEqualTo(request.refusjonsperioder().getFirst().tom());


    }

}
