package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingStatus;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingApiMottakTjenesteEtterkontrollTest {

    private static final String ORGNR = "999999999";
    private static final LocalDate STARTDATO = LocalDate.of(2024, 1, 1);
    private static final BigDecimal INNTEKT = new BigDecimal("50000");
    private static final AktørIdEntitet AKTØR_ID = new AktørIdEntitet("1234567890123");
    private static final Long ETTERKONTROLL_IM_ID = 99L;

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;

    private InntektsmeldingApiMottakTjeneste tjeneste;
    private ForespørselEntitet forespørsel;
    private InntektsmeldingEntitet imEntitet;

    @BeforeEach
    void setUp() {
        tjeneste = new InntektsmeldingApiMottakTjeneste(
            forespørselBehandlingTjeneste, inntektsmeldingRepository, prosessTaskTjeneste, inntektTjeneste);
        forespørsel = ForespørselEntitet.builder()
            .medOrganisasjonsnummer(ORGNR)
            .medSkjæringstidspunkt(STARTDATO)
            .medAktørId(AKTØR_ID)
            .medYtelseType(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medForespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .build();
        imEntitet = lagImEntitet(List.of());
        when(inntektsmeldingRepository.hentInntektsmelding(ETTERKONTROLL_IM_ID)).thenReturn(imEntitet);
    }

    @Test
    void utdatert_im_hoppes_over() {
        imEntitet.setStatus(InntektsmeldingStatus.UTDATERT);

        tjeneste.kontrollerInntektsmeldingEtterNedetid(ETTERKONTROLL_IM_ID);

        verifyNoInteractions(inntektTjeneste, prosessTaskTjeneste, forespørselBehandlingTjeneste);
        verify(inntektsmeldingRepository, never()).oppdaterStatus(any(), any());
    }

    @Test
    void nedetid_setter_venter_vurdering_og_kaster_exception() {
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(lagInntektsopplysningerMedNedetid());
        var statusCaptor = ArgumentCaptor.forClass(InntektsmeldingStatus.class);

        assertThrows(TekniskException.class,
            () -> tjeneste.kontrollerInntektsmeldingEtterNedetid(ETTERKONTROLL_IM_ID));

        verify(inntektsmeldingRepository).oppdaterStatus(any(), statusCaptor.capture());
        assertThat(statusCaptor.getValue()).isEqualTo(InntektsmeldingStatus.VENTER_VURDERING);
        verifyNoInteractions(prosessTaskTjeneste, forespørselBehandlingTjeneste);
    }

    @Test
    void ugyldig_inntekt_uten_årsak_setter_avvist_og_varsler() {
        // diff = |60100 - 50000| = 10100 > 50 (AKSEPTERT_AVVIK), ingen endringsårsaker
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(new BigDecimal("60100"), ORGNR, List.of()));
        var statusCaptor = ArgumentCaptor.forClass(InntektsmeldingStatus.class);

        tjeneste.kontrollerInntektsmeldingEtterNedetid(ETTERKONTROLL_IM_ID);

        verify(inntektsmeldingRepository).oppdaterStatus(any(), statusCaptor.capture());
        assertThat(statusCaptor.getValue()).isEqualTo(InntektsmeldingStatus.AVVIST);
        verify(forespørselBehandlingTjeneste).sendMeldingOmAvvistInntektsmelding(eq(forespørsel), any());
        verifyNoInteractions(prosessTaskTjeneste);
    }

    @Test
    void ugyldig_inntekt_med_årsak_godkjennes() {
        // diff = 10100 > 50, men endringsårsak er oppgitt → skal godkjennes
        var årsak = EndringsårsakEntitet.builder().medÅrsak(Endringsårsak.BONUS).build();
        when(inntektsmeldingRepository.hentInntektsmelding(ETTERKONTROLL_IM_ID))
            .thenReturn(lagImEntitet(List.of(årsak)));
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(new BigDecimal("60100"), ORGNR, List.of()));
        var statusCaptor = ArgumentCaptor.forClass(InntektsmeldingStatus.class);

        tjeneste.kontrollerInntektsmeldingEtterNedetid(ETTERKONTROLL_IM_ID);

        verify(inntektsmeldingRepository).oppdaterStatus(any(), statusCaptor.capture());
        assertThat(statusCaptor.getValue()).isEqualTo(InntektsmeldingStatus.GODKJENT);
        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getValue().taskType().value()).isEqualTo(SendTilJoarkTask.TASK_TYPE);
        verify(forespørselBehandlingTjeneste, never()).sendMeldingOmAvvistInntektsmelding(any(), any());
    }

    @Test
    void gyldig_inntekt_ferdigstiller_åpen_forespørsel() {
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(INNTEKT, ORGNR, List.of()));
        var statusCaptor = ArgumentCaptor.forClass(InntektsmeldingStatus.class);

        tjeneste.kontrollerInntektsmeldingEtterNedetid(ETTERKONTROLL_IM_ID);

        verify(inntektsmeldingRepository).oppdaterStatus(any(), statusCaptor.capture());
        assertThat(statusCaptor.getValue()).isEqualTo(InntektsmeldingStatus.GODKJENT);
        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getValue().taskType().value()).isEqualTo(SendTilJoarkTask.TASK_TYPE);
        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(any(), any(), any(), any(), any());
        verify(forespørselBehandlingTjeneste, never()).oppdaterPortalerMedEndretInntektsmelding(any(), any(), any());
    }

    @Test
    void gyldig_inntekt_oppdaterer_portaler_ved_ferdig_forespørsel() {
        forespørsel.setStatus(ForespørselStatus.FERDIG);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), any()))
            .thenReturn(new Inntektsopplysninger(INNTEKT, ORGNR, List.of()));
        var statusCaptor = ArgumentCaptor.forClass(InntektsmeldingStatus.class);

        tjeneste.kontrollerInntektsmeldingEtterNedetid(ETTERKONTROLL_IM_ID);

        verify(inntektsmeldingRepository).oppdaterStatus(any(), statusCaptor.capture());
        assertThat(statusCaptor.getValue()).isEqualTo(InntektsmeldingStatus.GODKJENT);
        var taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());
        assertThat(taskCaptor.getValue().taskType().value()).isEqualTo(SendTilJoarkTask.TASK_TYPE);
        verify(forespørselBehandlingTjeneste).oppdaterPortalerMedEndretInntektsmelding(eq(forespørsel), any(), any());
        verify(forespørselBehandlingTjeneste, never()).ferdigstillForespørsel(any(), any(), any(), any(), any());
    }

    // ---- Hjelpemetoder ----

    private InntektsmeldingEntitet lagImEntitet(List<EndringsårsakEntitet> endringsårsaker) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medArbeidsgiverIdent(ORGNR)
            .medMånedInntekt(INNTEKT)
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medInntektsmeldingType(InntektsmeldingType.ORDINÆR)
            .medStartDato(STARTDATO)
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medKontaktperson(new KontaktpersonEntitet("Ola Nordmann", "12345678"))
            .medEndringsårsaker(endringsårsaker)
            .medBortfaltNaturalytelser(List.of())
            .medRefusjonsendringer(List.of())
            .medStatus(InntektsmeldingStatus.VENTER_VURDERING)
            .medForespørsel(forespørsel)
            .build();
    }

    private Inntektsopplysninger lagInntektsopplysningerMedNedetid() {
        return new Inntektsopplysninger(null, ORGNR,
            List.of(new Inntektsopplysninger.InntektMåned(null, YearMonth.now().minusMonths(1), MånedslønnStatus.NEDETID_AINNTEKT)));
    }
}

