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
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.LpsSystemInfoEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
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
    void mapTilEntitet_omsorgspenger_fraværsperioder_hentes_fra_forespørsel_og_ikke_fra_request() {
        var periode1 = new no.nav.familie.inntektsmelding.typer.dto.PeriodeDto(
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 5));
        var periode2 = new no.nav.familie.inntektsmelding.typer.dto.PeriodeDto(
            LocalDate.of(2024, 3, 10), LocalDate.of(2024, 3, 12));

        var forespørsel = ForespørselEntitet.builder()
            .medOrganisasjonsnummer("999999999")
            .medSkjæringstidspunkt(STARTDATO)
            .medAktørId(AKTØR_ID)
            .medYtelseType(Ytelsetype.OMSORGSPENGER)
            .medForespørselType(ForespørselType.OMSORGSPENGER_REFUSJON)
            .medEtterspurtePerioder(List.of(periode1, periode2))
            .build();

        // request inneholder ingen omsorgspenger-data — entiteten skal likevel få fraværsperiodene fra forespørselen
        var request = lagRequestInntektsmelding(null, null, null);
        var entitet = InntektsmeldingApiMapper.mapTilEntitet(request, AKTØR_ID, forespørsel);

        var omsorgspenger = entitet.getOmsorgspenger();
        assertThat(omsorgspenger).isNotNull();
        assertThat(omsorgspenger.isHarUtbetaltPliktigeDager()).isTrue();

        var fraværsPerioder = omsorgspenger.getFraværsPerioder();
        assertThat(fraværsPerioder).hasSize(2);
        assertThat(fraværsPerioder.stream().map(fp -> fp.getPeriode().getFom()).toList())
            .containsExactlyInAnyOrder(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 10));
        assertThat(fraværsPerioder.stream().map(fp -> fp.getPeriode().getTom()).toList())
            .containsExactlyInAnyOrder(LocalDate.of(2024, 3, 5), LocalDate.of(2024, 3, 12));
    }

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
            new AvsenderSystemDto("TestSystem", "1.0")
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

    // -----------------------------------------------------------------------
    // mapOmsorgspengerTilKontrakt (nås via mapFraEntitet)
    // -----------------------------------------------------------------------

    @Test
    void mapFraEntitet_fraværHeleDager_mappesTilKorrektePeriodeDto() {
        var periode1 = PeriodeEntitet.fraOgMedTilOgMed(STARTDATO, STARTDATO.plusDays(2));
        var periode2 = PeriodeEntitet.fraOgMedTilOgMed(STARTDATO.plusDays(10), STARTDATO.plusDays(12));
        var omsorgspenger = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true)
            .medFraværsPerioder(List.of(new FraværsPeriodeEntitet(periode1), new FraværsPeriodeEntitet(periode2)))
            .medDelvisFraværsPerioder(List.of())
            .build();

        var dto = mapOmsorgspengerFraEntitet(omsorgspenger);

        assertThat(dto.harUtbetaltPliktigeDager()).isTrue();
        assertThat(dto.fraværHeleDager()).hasSize(2);
        assertThat(dto.fraværHeleDager()).containsExactlyInAnyOrder(
            new PeriodeDto(STARTDATO, STARTDATO.plusDays(2)),
            new PeriodeDto(STARTDATO.plusDays(10), STARTDATO.plusDays(12))
        );
    }

    @Test
    void mapFraEntitet_fraværDelerAvDagen_filtrererKorrekt() {
        var delvis = List.of(
            new DelvisFraværsPeriodeEntitet(STARTDATO, new BigDecimal("3.5")),
            new DelvisFraværsPeriodeEntitet(STARTDATO.plusDays(1), new BigDecimal("2"))
        );
        var omsorgspenger = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(false)
            .medFraværsPerioder(List.of())
            .medDelvisFraværsPerioder(delvis)
            .build();

        var dto = mapOmsorgspengerFraEntitet(omsorgspenger);

        assertThat(dto.harUtbetaltPliktigeDager()).isFalse();
        assertThat(dto.fraværDelerAvDagen()).hasSize(2);
        assertThat(dto.fraværDelerAvDagen().stream().map(OmsorgspengerDto.FraværDelerAvDagenDto::dato).toList())
            .containsExactlyInAnyOrder(STARTDATO, STARTDATO.plusDays(1));
        assertThat(dto.trukketPerioder()).isEmpty();
    }

    @Test
    void mapFraEntitet_trukkedeDager_nullTimer_rekonstrueresSomSammenhengenePerioder() {
        // 3 sammenhengende + 1 separat dag → skal gi 2 perioder
        var trukket = List.of(
            new DelvisFraværsPeriodeEntitet(STARTDATO, BigDecimal.ZERO),
            new DelvisFraværsPeriodeEntitet(STARTDATO.plusDays(1), BigDecimal.ZERO),
            new DelvisFraværsPeriodeEntitet(STARTDATO.plusDays(2), BigDecimal.ZERO),
            new DelvisFraværsPeriodeEntitet(STARTDATO.plusDays(5), BigDecimal.ZERO)   // gap på 2 dager
        );
        var omsorgspenger = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true)
            .medFraværsPerioder(List.of())
            .medDelvisFraværsPerioder(trukket)
            .build();

        var dto = mapOmsorgspengerFraEntitet(omsorgspenger);

        assertThat(dto.fraværDelerAvDagen()).isEmpty();
        assertThat(dto.trukketPerioder()).hasSize(2);
        assertThat(dto.trukketPerioder()).containsExactlyInAnyOrder(
            new PeriodeDto(STARTDATO, STARTDATO.plusDays(2)),
            new PeriodeDto(STARTDATO.plusDays(5), STARTDATO.plusDays(5))
        );
    }

    @Test
    void mapFraEntitet_blandetDelvisOgTrukket_skillesKorrekt() {
        var delvisOgTrukket = List.of(
            new DelvisFraværsPeriodeEntitet(STARTDATO, new BigDecimal("4")),        // timer > 0 → fraværDelerAvDagen
            new DelvisFraværsPeriodeEntitet(STARTDATO.plusDays(1), BigDecimal.ZERO) // timer == 0 → trukket
        );
        var omsorgspenger = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(false)
            .medFraværsPerioder(List.of())
            .medDelvisFraværsPerioder(delvisOgTrukket)
            .build();

        var dto = mapOmsorgspengerFraEntitet(omsorgspenger);

        assertThat(dto.fraværDelerAvDagen()).hasSize(1);
        assertThat(dto.fraværDelerAvDagen().getFirst().dato()).isEqualTo(STARTDATO);
        assertThat(dto.fraværDelerAvDagen().getFirst().timer()).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(dto.trukketPerioder()).hasSize(1);
        assertThat(dto.trukketPerioder().getFirst()).isEqualTo(new PeriodeDto(STARTDATO.plusDays(1), STARTDATO.plusDays(1)));
    }

    @Test
    void mapFraEntitet_ingenOmsorgspenger_girNullIDto() {
        var entitet = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medArbeidsgiverIdent("999999999")
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medStartDato(STARTDATO)
            .medMånedInntekt(INNTEKT)
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medInntektsmeldingType(InntektsmeldingType.ORDINÆR)
            .medKontaktperson(new KontaktpersonEntitet("Test Testesen", "99999999"))
            .medEndringsårsaker(List.of())
            .medBortfaltNaturalytelser(List.of())
            .medRefusjonsendringer(List.of())
            .medLpsSystemInfo(LpsSystemInfoEntitet.builder().medNavn("TestSystem").medVersjon("1.0").build())
            .medForespørsel(ForespørselEntitet.builder()
                .medOrganisasjonsnummer("999999999")
                .medSkjæringstidspunkt(STARTDATO)
                .medAktørId(AKTØR_ID)
                .medYtelseType(Ytelsetype.PLEIEPENGER_SYKT_BARN)
                .medForespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
                .build())
            .build();

        var dto = InntektsmeldingApiMapper.mapFraEntitet(entitet, new PersonIdent("12345678901"));

        assertThat(dto.omsorgspenger()).isNull();
    }

    /** Bygger en minimal InntektsmeldingEntitet med gitt OmsorgspengerEntitet og kaller mapFraEntitet. */
    private static OmsorgspengerDto mapOmsorgspengerFraEntitet(OmsorgspengerEntitet omsorgspenger) {
        var inntektsmeldingEntitet = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medArbeidsgiverIdent("999999999")
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medStartDato(STARTDATO)
            .medMånedInntekt(INNTEKT)
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medInntektsmeldingType(InntektsmeldingType.OMSORGSPENGER_REFUSJON)
            .medKontaktperson(new KontaktpersonEntitet("Test Testesen", "99999999"))
            .medEndringsårsaker(List.of())
            .medBortfaltNaturalytelser(List.of())
            .medRefusjonsendringer(List.of())
            .medLpsSystemInfo(LpsSystemInfoEntitet.builder().medNavn("TestSystem").medVersjon("1.0").build())
            .medOmsorgspenger(omsorgspenger)
            .medForespørsel(lagForespørsel())
            .build();

        return InntektsmeldingApiMapper.mapFraEntitet(inntektsmeldingEntitet, new PersonIdent("12345678901")).omsorgspenger();
    }
}
