package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.OmsorgspengerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.PeriodeDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendRefusjonOmsorgspengerRequest;

class InntektsmeldingApiMapperTest {

    private static final AktørIdEntitet AKTØR_ID = new AktørIdEntitet("1234567890123");
    private static final LocalDate STARTDATO = LocalDate.of(2024, 1, 1);
    private static final BigDecimal INNTEKT = new BigDecimal("50000");

    @Test
    void mapTilEntitet_med_null_refusjon_gir_ingen_npe_og_tom_liste() {
        var request = lagRequestInntektsmelding(null, null, null);
        var forespørsel = lagForespørsel();

        assertThatNoException().isThrownBy(() -> InntektsmeldingApiMapper.mapTilEntitet(request, AKTØR_ID, forespørsel));

        var entitet = InntektsmeldingApiMapper.mapTilEntitet(request, AKTØR_ID, forespørsel);
        assertThat(entitet.getRefusjonsendringer()).isEmpty();
        assertThat(entitet.getBorfalteNaturalYtelser()).isEmpty();
        assertThat(entitet.getEndringsårsaker()).isEmpty();
        assertThat(entitet.getMånedRefusjon()).isNull();
    }

    @Test
    void mapTilEntitet_med_tom_refusjon_gir_ingen_npe_og_tom_liste() {
        var request = lagRequestInntektsmelding(List.of(), List.of(), List.of());
        var forespørsel = lagForespørsel();

        assertThatNoException().isThrownBy(() -> InntektsmeldingApiMapper.mapTilEntitet(request, AKTØR_ID, forespørsel));

        var entitet = InntektsmeldingApiMapper.mapTilEntitet(request, AKTØR_ID, forespørsel);
        assertThat(entitet.getRefusjonsendringer()).isEmpty();
        assertThat(entitet.getBorfalteNaturalYtelser()).isEmpty();
        assertThat(entitet.getEndringsårsaker()).isEmpty();
        assertThat(entitet.getMånedRefusjon()).isNull();
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(INNTEKT);
    }

    @Test
    void mapTilEntitet_med_null_bortfaltNaturalytelse_og_null_endringsårsaker_gir_tomme_lister() {
        var refusjon = List.of(new no.nav.k9.inntektsmelding.felles.RefusjonDto(STARTDATO, INNTEKT));
        var request = lagRequestInntektsmelding(refusjon, null, null);
        var forespørsel = lagForespørsel();

        var entitet = InntektsmeldingApiMapper.mapTilEntitet(request, AKTØR_ID, forespørsel);

        assertThat(entitet.getBorfalteNaturalYtelser()).isEmpty();
        assertThat(entitet.getEndringsårsaker()).isEmpty();
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(INNTEKT);
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(INNTEKT);
    }

    @Test
    void mapTilEntitet_OmsorgspengerRefusjon_med_null_endringsårsaker_gir_ingen_npe_og_tom_liste() {
        var omsorgspenger = new OmsorgspengerDto(true, List.of(new PeriodeDto(STARTDATO, STARTDATO.plusDays(2))), List.of(), null);
        var request = lagRequestOmsorgspengerRefusjon(omsorgspenger, null);
        var forespørsel = lagForespørsel();

        assertThatNoException().isThrownBy(() -> InntektsmeldingApiMapper.mapTilEntitetOmsorgspengerRefusjon(request, AKTØR_ID, forespørsel));

        var entitet = InntektsmeldingApiMapper.mapTilEntitetOmsorgspengerRefusjon(request, AKTØR_ID, forespørsel);
        assertThat(entitet.getEndringsårsaker()).isEmpty();
        assertThat(entitet.getBorfalteNaturalYtelser()).isEmpty();
        assertThat(entitet.getRefusjonsendringer()).isEmpty();
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(INNTEKT);
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(INNTEKT);
    }

    @Test
    void mapTilEntitet_OmsorgspengerRefusjon_med_tom_endringsårsaker_gir_tom_liste() {
        var omsorgspenger = new OmsorgspengerDto(true, List.of(new PeriodeDto(STARTDATO, STARTDATO.plusDays(2))), List.of(), null);
        var request = lagRequestOmsorgspengerRefusjon(omsorgspenger, List.of());
        var forespørsel = lagForespørsel();

        var entitet = InntektsmeldingApiMapper.mapTilEntitetOmsorgspengerRefusjon(request, AKTØR_ID, forespørsel);

        assertThat(entitet.getEndringsårsaker()).isEmpty();
        assertThat(entitet.getMånedRefusjon()).isEqualByComparingTo(INNTEKT);
        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(INNTEKT);
    }

    @Test
    void mapTilEntitet_trukketPerioder_ekspanderes_til_delvis_fraværsdager_med_null_timerOmsorgspengerRefusjon() {
        var trukket = List.of(new PeriodeDto(STARTDATO, STARTDATO.plusDays(2))); // 3 dager
        var omsorgspenger = new OmsorgspengerDto(true, null, null, trukket);
        var request = lagRequestOmsorgspengerRefusjon(omsorgspenger, List.of());
        var forespørsel = lagForespørsel();

        var entitet = InntektsmeldingApiMapper.mapTilEntitetOmsorgspengerRefusjon(request, AKTØR_ID, forespørsel);
        var delvis = entitet.getOmsorgspenger().getDelvisFraværsPerioder();

        assertThat(entitet.getMånedInntekt()).isEqualByComparingTo(INNTEKT);
        assertThat(delvis).hasSize(3);
        assertThat(delvis).allMatch(d -> d.getTimer().compareTo(BigDecimal.ZERO) == 0);
        assertThat(delvis.stream().map(DelvisFraværsPeriodeEntitet::getDato).toList())
            .containsExactlyInAnyOrder(STARTDATO, STARTDATO.plusDays(1), STARTDATO.plusDays(2));
    }

    private static SendInntektsmeldingRequest lagRequestInntektsmelding(
        List<no.nav.k9.inntektsmelding.felles.RefusjonDto> refusjon,
        List<no.nav.k9.inntektsmelding.felles.BortfaltNaturalytelseDto> bortfalte,
        List<no.nav.k9.inntektsmelding.felles.EndringsårsakerDto> endringsårsaker) {

        var omsorgspenger = new OmsorgspengerDto(
            true,
            List.of(new PeriodeDto(STARTDATO, STARTDATO.plusDays(2))),
            List.of(),
            List.of()
        );
        return new SendInntektsmeldingRequest(
            UUID.randomUUID(),
            new FødselsnummerDto("12345678901"),
            new OrganisasjonsnummerDto("999999999"),
            STARTDATO,
            YtelseTypeDto.OMSORGSPENGER,
            new KontaktpersonDto("Ola Nordmann", "12345678"),
            INNTEKT,
            refusjon,
            bortfalte,
            endringsårsaker,
            new AvsenderSystemDto("TestSystem", "1.0"),
            omsorgspenger
        );
    }

    private static SendRefusjonOmsorgspengerRequest lagRequestOmsorgspengerRefusjon(OmsorgspengerDto omsorgspenger,
                                                                                    List<EndringsårsakerDto> endringsårsakerDto) {
        return new SendRefusjonOmsorgspengerRequest(
            new FødselsnummerDto("12345678901"),
            new OrganisasjonsnummerDto("999999999"),
            STARTDATO,
            new KontaktpersonDto("Ola Nordmann", "12345678"),
            INNTEKT,
            endringsårsakerDto,
            new AvsenderSystemDto("TestSystem", "1.0"),
            omsorgspenger
        );
    }

    private static ForespørselEntitet lagForespørsel() {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer("999999999")
            .medSkjæringstidspunkt(STARTDATO)
            .medAktørId(AKTØR_ID)
            .medYtelseType(Ytelsetype.OMSORGSPENGER)
            .medForespørselType(ForespørselType.OMSORGSPENGER_REFUSJON)
            .build();
    }
}
