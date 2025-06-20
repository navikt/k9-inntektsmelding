package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselMapper;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

class SettForespørselTilUtgåttTaskTest {

    private final ForespørselEntitet entitet = ForespørselMapper.mapForespørsel("arbeidsgiverOrgNr", LocalDate.now(), "1234567890134", Ytelsetype.PLEIEPENGER_SYKT_BARN, "saksnummer", LocalDate.now(), null);
    private final UUID forespørselUuid = entitet.getUuid();

    private final ForespørselBehandlingTjeneste forespørselBehandlingTjeneste = Mockito.mock(ForespørselBehandlingTjeneste.class);

    @Test
    void skal_sette_forespørsel_til_utgått() {
        var task = new SettForespørselTilUtgåttTask(forespørselBehandlingTjeneste);
        var taskdata = ProsessTaskData.forProsessTask(SettForespørselTilUtgåttTask.class);
        taskdata.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, forespørselUuid.toString());

        entitet.setStatus(ForespørselStatus.UNDER_BEHANDLING);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid))
            .thenReturn(Optional.of(entitet));

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste).settForespørselTilUtgått(entitet, true);
    }

    @Test
    void skal_ikke_sette_ferdig_forespørsel_til_utgått() {
        var task = new SettForespørselTilUtgåttTask(forespørselBehandlingTjeneste);
        var taskdata = ProsessTaskData.forProsessTask(SettForespørselTilUtgåttTask.class);
        taskdata.setProperty(SettForespørselTilUtgåttTask.FORESPØRSEL_UUID, forespørselUuid.toString());

        entitet.setStatus(ForespørselStatus.FERDIG);
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid))
            .thenReturn(Optional.of(entitet));

        task.doTask(taskdata);

        verify(forespørselBehandlingTjeneste, times(0)).settForespørselTilUtgått(any(ForespørselEntitet.class), eq(true));
    }
}
