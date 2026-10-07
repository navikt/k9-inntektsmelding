package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselMapper;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

class FerdigstillForespørselTaskTest {

    private static final String ORGNR = "974760673";
    private static final String AKTØR_ID = "1234567890134";

    private final ForespørselEntitet forespørsel = ForespørselMapper.mapForespørsel(ORGNR, LocalDate.now(), AKTØR_ID,
        Ytelsetype.PLEIEPENGER_SYKT_BARN, "saksnummer", ForespørselType.BESTILT_AV_FAGSYSTEM, LocalDate.now(), null);
    private final UUID forespørselUuid = forespørsel.getUuid();

    private final ForespørselBehandlingTjeneste forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);
    private final InntektsmeldingRepository inntektsmeldingRepository = Mockito.mock(InntektsmeldingRepository.class);
    private final FerdigstillForespørselTask task = new FerdigstillForespørselTask(forespørselBehandlingTjeneste, inntektsmeldingRepository);

    @Test
    void skal_ferdigstille_forespørsel_med_inntektsmelding() {
        var inntektsmelding = Mockito.mock(InntektsmeldingEntitet.class);
        var imUuid = UUID.randomUUID();
        when(inntektsmeldingRepository.hentInntektsmeldingForUuid(imUuid)).thenReturn(Optional.of(inntektsmelding));
        forespørsel.setStatus(ForespørselStatus.UNDER_BEHANDLING);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));
        when(forespørselBehandlingTjeneste.ferdigstillForespørsel(any(), any(), any(), any(), any())).thenReturn(forespørsel);

        task.doTask(FerdigstillForespørselTask.lagTaskData(forespørsel, Optional.of(imUuid), LukkeÅrsak.ORDINÆR_INNSENDING));

        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(forespørselUuid, forespørsel.getAktørId(), new OrganisasjonsnummerDto(ORGNR),
            LukkeÅrsak.ORDINÆR_INNSENDING, Optional.of(inntektsmelding));
    }

    @Test
    void skal_ferdigstille_forespørsel_uten_inntektsmelding_ved_ekstern_innsending() {
        forespørsel.setStatus(ForespørselStatus.UNDER_BEHANDLING);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));
        when(forespørselBehandlingTjeneste.ferdigstillForespørsel(any(), any(), any(), any(), any())).thenReturn(forespørsel);

        task.doTask(FerdigstillForespørselTask.lagTaskData(forespørsel, Optional.empty(), LukkeÅrsak.EKSTERN_INNSENDING));

        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(forespørselUuid, forespørsel.getAktørId(), new OrganisasjonsnummerDto(ORGNR),
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());
        verify(inntektsmeldingRepository, never()).hentInntektsmeldingForUuid(any());
    }


    // Er vi sikre på at vi ønsker å ferdigstille forespørsel som allerede er utgått?
    @Test
    void skal_ferdigstille_utgått_forespørsel() {
        forespørsel.setStatus(ForespørselStatus.UTGÅTT);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));
        when(forespørselBehandlingTjeneste.ferdigstillForespørsel(any(), any(), any(), any(), any())).thenReturn(forespørsel);

        task.doTask(FerdigstillForespørselTask.lagTaskData(forespørsel, Optional.empty(), LukkeÅrsak.EKSTERN_INNSENDING));

        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(any(), any(), any(), any(), any());
    }

    @Test
    void skal_ikke_ferdigstille_forespørsel_som_allerede_er_ferdig() {
        forespørsel.setStatus(ForespørselStatus.FERDIG);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.of(forespørsel));

        task.doTask(FerdigstillForespørselTask.lagTaskData(forespørsel, Optional.empty(), LukkeÅrsak.EKSTERN_INNSENDING));

        verify(forespørselBehandlingTjeneste, never()).ferdigstillForespørsel(any(), any(), any(), any(), any());
    }

    @Test
    void skal_ikke_feile_dersom_forespørsel_ikke_finnes() {
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(Optional.empty());

        task.doTask(FerdigstillForespørselTask.lagTaskData(forespørsel, Optional.empty(), LukkeÅrsak.EKSTERN_INNSENDING));

        verify(forespørselBehandlingTjeneste, never()).ferdigstillForespørsel(any(), any(), any(), any(), any());
    }

    @Test
    void skal_lage_taskdata_med_gruppe_og_saksnummer() {
        var imUuid = UUID.randomUUID();
        var taskdata = FerdigstillForespørselTask.lagTaskData(forespørsel, Optional.of(imUuid), LukkeÅrsak.ORDINÆR_INNSENDING);

        assertThat(taskdata.taskType()).isEqualTo(TaskType.forProsessTask(FerdigstillForespørselTask.class));
        assertThat(taskdata.getPropertyValue(HåndterRekkefølgeAvForespørselTasks.FORESPØRSEL_UUID)).isEqualTo(forespørselUuid.toString());
        assertThat(taskdata.getPropertyValue(FerdigstillForespørselTask.INNTEKTSMELDING_UUID)).isEqualTo(imUuid.toString());
        assertThat(taskdata.getPropertyValue(FerdigstillForespørselTask.LUKKE_ÅRSAK)).isEqualTo(LukkeÅrsak.ORDINÆR_INNSENDING.name());
        assertThat(taskdata.getSaksnummer()).isEqualTo("saksnummer");
        assertThat(taskdata.getGruppe()).isEqualTo(forespørselUuid.toString());
        assertThat(Long.parseLong(taskdata.getSekvens())).isPositive();
    }
}
