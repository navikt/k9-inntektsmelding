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
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID(), new AktørIdDto("9999999999999"), YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"), new SendInntektsmeldingRequestDto.KontaktpersonDto("Testy test", "999999999"), LocalDate.now(),
            BigDecimal.valueOf(5000), null, Collections.emptyList(), Collections.emptyList());

        // Act
        var entitet = InntektsmeldingMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(entitet.getStartDato()).isEqualTo(request.startdato());
        assertThat(entitet.getYtelsetype()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo(request.kontaktperson().navn());
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());
        assertThat(entitet.getNaturalYtelser()).isEmpty();
        assertThat(entitet.getMånedRefusjon()).isNull();
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(Tid.TIDENES_ENDE);

    }

    @Test
    void skal_teste_mapping_med_ref_og_naturalytelse() {
        // Arrange
        var request = new SendInntektsmeldingRequestDto(UUID.randomUUID(), new AktørIdDto("9999999999999"), YtelseTypeDto.FORELDREPENGER,
            new ArbeidsgiverDto("999999999"), new SendInntektsmeldingRequestDto.KontaktpersonDto("Testy test", "999999999"), LocalDate.now(),
            BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), Collections.singletonList(new SendInntektsmeldingRequestDto.RefusjonendringRequestDto(LocalDate.now().plusDays(10), BigDecimal.ZERO)),
            Collections.singletonList(
                new SendInntektsmeldingRequestDto.NaturalytelseRequestDto(LocalDate.now(), Tid.TIDENES_ENDE, NaturalytelsetypeDto.ANNET, true,
                    BigDecimal.valueOf(4000))));

        // Act
        var entitet = InntektsmeldingMapper.mapTilEntitet(request);

        // Assert
        assertThat(entitet.getAktørId().getAktørId()).isEqualTo(request.aktorId().id());
        assertThat(entitet.getArbeidsgiverIdent()).isEqualTo(request.arbeidsgiverIdent().ident());
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(request.inntekt());
        assertThat(entitet.getStartDato()).isEqualTo(request.startdato());
        assertThat(entitet.getYtelsetype()).isEqualTo(KodeverkMapper.mapYtelsetype(request.ytelse()));
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(entitet.getOpphørsdatoRefusjon()).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(entitet.getKontaktperson().getNavn()).isEqualTo(request.kontaktperson().navn());
        assertThat(entitet.getKontaktperson().getTelefonnummer()).isEqualTo(request.kontaktperson().telefonnummer());


        assertThat(entitet.getNaturalYtelser()).hasSize(1);
        assertThat(entitet.getNaturalYtelser().getFirst().getBeløp()).isEqualByComparingTo(
            request.bortfaltNaturalytelsePerioder().getFirst().beløp());
        assertThat(entitet.getNaturalYtelser().getFirst().getType()).isEqualByComparingTo(
            KodeverkMapper.mapNaturalytelseTilEntitet(request.bortfaltNaturalytelsePerioder().getFirst().naturalytelsetype()));
        assertThat(entitet.getNaturalYtelser().getFirst().getPeriode().getFom()).isEqualTo(request.bortfaltNaturalytelsePerioder().getFirst().fom());
        assertThat(entitet.getNaturalYtelser().getFirst().getPeriode().getTom()).isEqualTo(request.bortfaltNaturalytelsePerioder().getFirst().tom());
        assertThat(entitet.getNaturalYtelser().getFirst().getErBortfalt()).isEqualTo(
            request.bortfaltNaturalytelsePerioder().getFirst().erBortfalt());
    }

}
