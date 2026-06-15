package no.nav.familie.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselMapper;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequest;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.KontaktpersonDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingMottakTjenesteTest {

    private static final String INNMELDER_UID = "12324312345";
    private static final String ORGNR = "999999999";
    private static final String AKTØR_ID_STR = "9999999999999";
    private static final UUID FORESPORSEL_UUID = UUID.randomUUID();
    private static final LocalDate STARTDATO = LocalDate.of(2024, 1, 1);

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private InntektsmeldingMottakTjeneste inntektsmeldingMottakTjeneste;

    @BeforeAll
    static void beforeAll() {
        KontekstHolder.setKontekst(RequestKontekst.forRequest(INNMELDER_UID, "kompakt", IdentType.EksternBruker,
            new OpenIDToken(OpenIDProvider.TOKENX, new TokenString("token")), UUID.randomUUID(), Set.of()));
    }

    @AfterAll
    static void afterAll() {
        KontekstHolder.fjernKontekst();
    }

    @BeforeEach
    void setUp() {
        inntektsmeldingMottakTjeneste = new InntektsmeldingMottakTjeneste(forespørselBehandlingTjeneste, inntektsmeldingRepository, prosessTaskTjeneste);
    }


    @Test
    void skal_ikke_godta_im_på_utgått_forespørrsel() {
        // Arrange
        var forespørsel = ForespørselMapper.mapForespørsel(ORGNR, STARTDATO, AKTØR_ID_STR,
            Ytelsetype.PLEIEPENGER_SYKT_BARN, "123", ForespørselType.BESTILT_AV_FAGSYSTEM, null, null);
        forespørsel.setStatus(ForespørselStatus.UTGÅTT);
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));

        // Act
        var ex = assertThrows(IllegalStateException.class, () -> inntektsmeldingMottakTjeneste.mottaInntektsmelding(lagRequest()));

        // Assert
        assertThat(ex.getMessage()).contains("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
    }

    @Test
    void skal_ferdigstille_forespørsel_ved_første_innsending() {
        // Arrange
        var forespørsel = spy(lagForespørsel());
        var lagretIm = lagInntektsmeldingEntitet(forespørsel);
        when(forespørsel.getInntektsmeldinger()).thenReturn(List.of(lagretIm)); // size == 1 → første innsending
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));
        when(forespørselBehandlingTjeneste.ferdigstillForespørsel(any(), any(), any(), any(), any())).thenReturn(forespørsel);
        stubLagring(lagretIm);

        // Act
        inntektsmeldingMottakTjeneste.mottaInntektsmelding(lagRequest());

        // Assert
        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(eq(FORESPORSEL_UUID), any(), any(), eq(LukkeÅrsak.ORDINÆR_INNSENDING), any());
        verify(forespørselBehandlingTjeneste, never()).oppdaterPortalerMedEndretInntektsmelding(any(), any(), any());
    }

    @Test
    void skal_oppdatere_portaler_ved_reinnsending() {
        // Arrange
        var forespørsel = spy(lagForespørsel());
        var eksisterendeIm = lagInntektsmeldingEntitet(forespørsel);
        var nyIm = lagInntektsmeldingEntitet(forespørsel);
        when(forespørsel.getInntektsmeldinger()).thenReturn(List.of(eksisterendeIm, nyIm)); // size > 1 → reinnsending
        when(forespørselBehandlingTjeneste.hentForespørsel(FORESPORSEL_UUID)).thenReturn(Optional.of(forespørsel));
        stubLagring(nyIm);

        // Act
        inntektsmeldingMottakTjeneste.mottaInntektsmelding(lagRequest());

        // Assert
        verify(forespørselBehandlingTjeneste).oppdaterPortalerMedEndretInntektsmelding(eq(forespørsel), any(), any());
        verify(forespørselBehandlingTjeneste, never()).ferdigstillForespørsel(any(), any(), any(), any(), any());
    }

    // ---- Hjelpemetoder ----

    private ForespørselEntitet lagForespørsel() {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer(ORGNR)
            .medSkjæringstidspunkt(STARTDATO)
            .medAktørId(new AktørIdEntitet(AKTØR_ID_STR))
            .medYtelseType(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medForespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .build();
    }

    private InntektsmeldingEntitet lagInntektsmeldingEntitet(ForespørselEntitet forespørsel) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(AKTØR_ID_STR))
            .medArbeidsgiverIdent(ORGNR)
            .medMånedInntekt(BigDecimal.valueOf(50000))
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medInntektsmeldingType(InntektsmeldingType.ORDINÆR)
            .medStartDato(STARTDATO)
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medKontaktperson(new KontaktpersonEntitet("Test Testersen", "12345678"))
            .medEndringsårsaker(List.of())
            .medBortfaltNaturalytelser(List.of())
            .medRefusjonsendringer(List.of())
            .medForespørsel(forespørsel)
            .build();
    }

    private SendInntektsmeldingRequest lagRequest() {
        return new SendInntektsmeldingRequest(
            FORESPORSEL_UUID,
            new AktørIdDto(AKTØR_ID_STR),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new ArbeidsgiverDto(ORGNR),
            new KontaktpersonDto("Test Testersen", "12345678"),
            STARTDATO,
            BigDecimal.valueOf(50000),
            List.of(),
            List.of(),
            List.of(),
            null);
    }

    private void stubLagring(InntektsmeldingEntitet lagretEntitet) {
        when(inntektsmeldingRepository.lagreInntektsmelding(any())).thenReturn(1L);
        when(inntektsmeldingRepository.hentInntektsmelding(1L)).thenReturn(lagretEntitet);
    }
}
