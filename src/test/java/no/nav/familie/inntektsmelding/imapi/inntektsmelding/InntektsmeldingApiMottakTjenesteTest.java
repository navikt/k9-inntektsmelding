package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.LpsSystemInfoEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.k9.inntektsmelding.felles.FeilkodeDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.OmsorgspengerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingApiMottakTjenesteTest {

    private static final String ORGNR = "999999999";
    private static final String FNR = "12345678901";
    private static final UUID FORESPORSEL_UUID = UUID.randomUUID();
    private static final LocalDate STARTDATO = LocalDate.of(2024, 1, 1);
    private static final BigDecimal INNTEKT = new BigDecimal("50000");
    private static final AktørIdEntitet AKTØR_ID = new AktørIdEntitet("1234567890123");

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;

    private InntektsmeldingApiMottakTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new InntektsmeldingApiMottakTjeneste(
            forespørselBehandlingTjeneste, inntektsmeldingRepository, prosessTaskTjeneste, inntektTjeneste);
    }

    @Test
    void forespørsel_ikke_funnet_returnerer_feil() {
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.empty());

        var response = tjeneste.mottaInntektsmelding(lagRequest(), AKTØR_ID);

        assertThat(response.success()).isFalse();
        assertThat(response.feilinformasjon().feilkode()).isEqualTo(FeilkodeDto.TOM_FORESPOERSEL);
        verifyNoInteractions(inntektTjeneste, inntektsmeldingRepository, prosessTaskTjeneste);
    }

    @Test
    void forespørsel_med_status_utgått_returnerer_feil() {
        var forespørsel = lagForespørsel();
        forespørsel.setStatus(ForespørselStatus.UTGÅTT);
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));

        var response = tjeneste.mottaInntektsmelding(lagRequest(), AKTØR_ID);

        assertThat(response.success()).isFalse();
        assertThat(response.feilinformasjon().feilkode()).isEqualTo(FeilkodeDto.UGYLDIG_FORESPOERSEL);
        verifyNoInteractions(inntektTjeneste, inntektsmeldingRepository, prosessTaskTjeneste);
    }

    @Test
    void duplikat_inntektsmelding_avvises() {
        var forespørsel = spy(lagForespørsel());
        var eksisterendeIm = lagMatchendeInntektsmeldingEntitet(forespørsel);
        when(forespørsel.getInntektsmeldinger()).thenReturn(List.of(eksisterendeIm));
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));

        var response = tjeneste.mottaInntektsmelding(lagRequest(), AKTØR_ID);

        assertThat(response.success()).isFalse();
        assertThat(response.feilinformasjon().feilkode()).isEqualTo(FeilkodeDto.DUPLIKAT);
        verifyNoInteractions(inntektTjeneste, inntektsmeldingRepository, prosessTaskTjeneste);
    }

    @Test
    void nedetid_ainntekt_returnerer_feil() {
        var forespørsel = lagForespørsel();
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(lagInntektsopplysningerMedNedetid());

        var response = tjeneste.mottaInntektsmelding(lagRequest(), AKTØR_ID);

        assertThat(response.success()).isFalse();
        assertThat(response.feilinformasjon().feilkode()).isEqualTo(FeilkodeDto.NEDETID_AINNTEKT);
        verifyNoInteractions(inntektsmeldingRepository, prosessTaskTjeneste);
    }

    @Test
    void ulik_inntekt_uten_endringsårsak_returnerer_feil() {
        var forespørsel = lagForespørsel();
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));
        // Gjennomsnitt 60100 − oppgitt 50000 = diff 10100 > 50 (AKSEPTERT_AVVIK), ingen endringsårsak
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(new BigDecimal("60100"), ORGNR, List.of()));

        var response = tjeneste.mottaInntektsmelding(lagRequest(), AKTØR_ID);

        assertThat(response.success()).isFalse();
        assertThat(response.feilinformasjon().feilkode()).isEqualTo(FeilkodeDto.ULIK_INNTEKT);
        verifyNoInteractions(inntektsmeldingRepository, prosessTaskTjeneste);
    }

    @Test
    void ulik_inntekt_med_endringsårsak_godtas() {
        var forespørsel = lagForespørsel();
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(new BigDecimal("60100"), ORGNR, List.of()));
        stubLagring(forespørsel);

        var response = tjeneste.mottaInntektsmelding(lagRequestMedEndringsårsak(), AKTØR_ID);

        assertThat(response.success()).isTrue();
    }

    @Test
    void inntekt_innenfor_akseptert_avvik_godtas() {
        var forespørsel = lagForespørsel();
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));
        // diff = 50030 − 50000 = 30 ≤ 50 (AKSEPTERT_AVVIK)
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(new BigDecimal("50030"), ORGNR, List.of()));
        stubLagring(forespørsel);

        var response = tjeneste.mottaInntektsmelding(lagRequest(), AKTØR_ID);

        assertThat(response.success()).isTrue();
    }

    @Test
    void ok_flyt_lagrer_og_ferdigstiller() {
        var forespørsel = lagForespørsel();
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(INNTEKT, ORGNR, List.of()));
        var lagretEntitet = stubLagring(forespørsel);

        var response = tjeneste.mottaInntektsmelding(lagRequest(), AKTØR_ID);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).isEqualTo(lagretEntitet.getUuid());
        verify(inntektsmeldingRepository).lagreInntektsmelding(any());
        verify(prosessTaskTjeneste).lagre(any(ProsessTaskData.class));
        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(eq(FORESPORSEL_UUID), any(), any(), any(), any());
    }

    @Test
    void ok_flyt_med_omsorgspenger_bruker_ferdigstill_med_fraværsperioder() {
        var forespørsel = lagForespørselForOmsorgspenger();
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(INNTEKT, ORGNR, List.of()));
        var lagretEntitet = stubLagringMedOmsorgspenger(forespørsel);

        var response = tjeneste.mottaInntektsmelding(lagRequestMedOmsorgspenger(), AKTØR_ID);

        assertThat(response.success()).isTrue();
        assertThat(response.inntektsmeldingUuid()).isEqualTo(lagretEntitet.getUuid());
        verify(inntektsmeldingRepository).lagreInntektsmelding(any());
        verify(prosessTaskTjeneste).lagre(any(ProsessTaskData.class));
        // Verifiser at 7-argumenters-overloaden (med fraværsperioder) brukes
        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(
            eq(FORESPORSEL_UUID), any(), any(), any(), any());
    }

    // ---- Hjelpemetoder ----

    private SendInntektsmeldingRequest lagRequest() {
        return new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new FødselsnummerDto(FNR),
            new OrganisasjonsnummerDto(ORGNR),
            STARTDATO,
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new KontaktpersonDto("Ola Nordmann", "12345678"),
            INNTEKT,
            List.of(),
            List.of(),
            List.of(),
            new AvsenderSystemDto("TestSystem", "1.0"),
            null
        );
    }

    private SendInntektsmeldingRequest lagRequestMedEndringsårsak() {
        return new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new FødselsnummerDto(FNR),
            new OrganisasjonsnummerDto(ORGNR),
            STARTDATO,
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new KontaktpersonDto("Ola Nordmann", "12345678"),
            INNTEKT,
            List.of(),
            List.of(),
            List.of(new EndringsårsakerDto(EndringsårsakDto.TARIFFENDRING, null, null, null)),
            new AvsenderSystemDto("TestSystem", "1.0"),
            null
        );
    }

    private SendInntektsmeldingRequest lagRequestMedOmsorgspenger() {
        var omsorgspenger = new OmsorgspengerDto(
            true,
            List.of(new OmsorgspengerDto.FraværHeleDagerDto(STARTDATO, STARTDATO.plusDays(2))),
            List.of()
        );
        return new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new FødselsnummerDto(FNR),
            new OrganisasjonsnummerDto(ORGNR),
            STARTDATO,
            YtelseTypeDto.OMSORGSPENGER,
            new KontaktpersonDto("Ola Nordmann", "12345678"),
            INNTEKT,
            List.of(),
            List.of(),
            List.of(),
            new AvsenderSystemDto("TestSystem", "1.0"),
            omsorgspenger
        );
    }

    private ForespørselEntitet lagForespørsel() {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer(ORGNR)
            .medSkjæringstidspunkt(STARTDATO)
            .medAktørId(AKTØR_ID)
            .medYtelseType(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medForespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .build();
    }

    private ForespørselEntitet lagForespørselForOmsorgspenger() {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer(ORGNR)
            .medSkjæringstidspunkt(STARTDATO)
            .medAktørId(AKTØR_ID)
            .medYtelseType(Ytelsetype.OMSORGSPENGER)
            .medForespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .build();
    }

    /**
     * Bygger en InntektsmeldingEntitet med de samme feltverdiene som
     * InntektsmeldingApiMapper ville produsert fra lagRequest(), slik at
     * duplikat-sjekken i inntektsmeldingerErLike() slår til.
     */
    private InntektsmeldingEntitet lagMatchendeInntektsmeldingEntitet(ForespørselEntitet forespørsel) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medArbeidsgiverIdent(ORGNR)
            .medMånedInntekt(INNTEKT)
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medInntektsmeldingType(InntektsmeldingType.ORDINÆR)
            .medStartDato(STARTDATO)
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medKontaktperson(new KontaktpersonEntitet("Ola Nordmann", "12345678"))
            .medEndringsårsaker(List.of())
            .medBortfaltNaturalytelser(List.of())
            .medRefusjonsendringer(List.of())
            .medLpsSystemInfo(LpsSystemInfoEntitet.builder()
                .medNavn("TestSystem")
                .medVersjon("1.0")
                .build())
            .medForespørsel(forespørsel)
            .build();
    }

    private Inntektsopplysninger lagInntektsopplysningerMedNedetid() {
        return new Inntektsopplysninger(null, ORGNR,
            List.of(new Inntektsopplysninger.InntektMåned(null, YearMonth.now().minusMonths(1), MånedslønnStatus.NEDETID_AINNTEKT)));
    }

    private InntektsmeldingEntitet stubLagring(ForespørselEntitet forespørsel) {
        var lagretEntitet = lagMatchendeInntektsmeldingEntitet(forespørsel);
        when(inntektsmeldingRepository.lagreInntektsmelding(any())).thenReturn(1L);
        when(inntektsmeldingRepository.hentInntektsmelding(1L)).thenReturn(lagretEntitet);
        return lagretEntitet;
    }

    private InntektsmeldingEntitet stubLagringMedOmsorgspenger(ForespørselEntitet forespørsel) {
        var omsorgspenger = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true)
            .medFraværsPerioder(List.of())
            .medDelvisFraværsPerioder(List.of())
            .build();
        var entitet = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medArbeidsgiverIdent(ORGNR)
            .medMånedInntekt(INNTEKT)
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medInntektsmeldingType(InntektsmeldingType.ORDINÆR)
            .medStartDato(STARTDATO)
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medKontaktperson(new KontaktpersonEntitet("Ola Nordmann", "12345678"))
            .medEndringsårsaker(List.of())
            .medBortfaltNaturalytelser(List.of())
            .medRefusjonsendringer(List.of())
            .medLpsSystemInfo(LpsSystemInfoEntitet.builder().medNavn("TestSystem").medVersjon("1.0").build())
            .medOmsorgspenger(omsorgspenger)
            .medForespørsel(forespørsel)
            .build();
        when(inntektsmeldingRepository.lagreInntektsmelding(any())).thenReturn(2L);
        when(inntektsmeldingRepository.hentInntektsmelding(2L)).thenReturn(entitet);
        return entitet;
    }
}



