package no.nav.familie.inntektsmelding.metrikker.bigquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class PubliserHistoriskeInntektsmeldingerStatistikkTaskTest {

    private static final long LOEPENR_TIL = 1000L;

    @Mock
    private InntektsmeldingRepository inntektsmeldingRepository;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private PubliserHistoriskeInntektsmeldingerStatistikkTask task;

    @BeforeEach
    void setUp() {
        task = new PubliserHistoriskeInntektsmeldingerStatistikkTask(inntektsmeldingRepository, prosessTaskTjeneste);
    }

    @Test
    void skal_ikke_opprette_tasker_når_batch_er_tom() {
        when(inntektsmeldingRepository.hentGodkjenteInntektsmeldingerMellomLoepenr(0L, LOEPENR_TIL, 2)).thenReturn(List.of());

        task.doTask(PubliserHistoriskeInntektsmeldingerStatistikkTask.opprettTask(0L, LOEPENR_TIL, 2, true));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_opprette_tasker_og_ikke_ny_batch_når_batch_ikke_er_full() {
        var inntektsmeldinger = lagInntektsmeldinger(11, 12);
        when(inntektsmeldingRepository.hentGodkjenteInntektsmeldingerMellomLoepenr(10L, LOEPENR_TIL, 3)).thenReturn(inntektsmeldinger);

        task.doTask(PubliserHistoriskeInntektsmeldingerStatistikkTask.opprettTask(10L, LOEPENR_TIL, 3, true));

        var gruppe = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(gruppe.capture());
        assertThat(gruppe.getValue().getTasks()).hasSize(2);
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_opprette_ny_batch_fra_siste_loepenr_når_batch_er_full_og_automatisk() {
        var inntektsmeldinger = lagInntektsmeldinger(11, 12);
        when(inntektsmeldingRepository.hentGodkjenteInntektsmeldingerMellomLoepenr(10L, LOEPENR_TIL, 2)).thenReturn(inntektsmeldinger);

        task.doTask(PubliserHistoriskeInntektsmeldingerStatistikkTask.opprettTask(10L, LOEPENR_TIL, 2, true));

        verify(prosessTaskTjeneste).lagre(any(ProsessTaskGruppe.class));
        var nyBatch = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(nyBatch.capture());
        assertThat(nyBatch.getValue().getPropertyValue(PubliserHistoriskeInntektsmeldingerStatistikkTask.LOEPENR_FRA)).isEqualTo("12");
        assertThat(nyBatch.getValue().getPropertyValue(PubliserHistoriskeInntektsmeldingerStatistikkTask.LOEPENR_TIL)).isEqualTo(String.valueOf(LOEPENR_TIL));
        assertThat(nyBatch.getValue().getPropertyValue(PubliserHistoriskeInntektsmeldingerStatistikkTask.BATCH_SIZE)).isEqualTo("2");
        assertThat(nyBatch.getValue().getPropertyValue(PubliserHistoriskeInntektsmeldingerStatistikkTask.AUTOMATISK_BATCH)).isEqualTo("true");
    }

    @Test
    void skal_ikke_opprette_ny_batch_når_batch_er_full_uten_automatisk() {
        var inntektsmeldinger = lagInntektsmeldinger(11, 12);
        when(inntektsmeldingRepository.hentGodkjenteInntektsmeldingerMellomLoepenr(10L, LOEPENR_TIL, 2)).thenReturn(inntektsmeldinger);

        task.doTask(PubliserHistoriskeInntektsmeldingerStatistikkTask.opprettTask(10L, LOEPENR_TIL, 2, false));

        verify(prosessTaskTjeneste).lagre(any(ProsessTaskGruppe.class));
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    private static List<InntektsmeldingEntitet> lagInntektsmeldinger(long fraLoepenr, long tilLoepenr) {
        return LongStream.rangeClosed(fraLoepenr, tilLoepenr).mapToObj(loepenr -> {
            var forespørsel = mock(ForespørselEntitet.class);
            when(forespørsel.getSaksnummer()).thenReturn(Optional.of("SAK" + loepenr));
            var im = mock(InntektsmeldingEntitet.class);
            when(im.getId()).thenReturn(loepenr * 100);
            when(im.getForespørsel()).thenReturn(forespørsel);
            lenient().when(im.getLoepenr()).thenReturn(loepenr);
            lenient().when(im.getUuid()).thenReturn(UUID.randomUUID());
            return im;
        }).toList();
    }
}
