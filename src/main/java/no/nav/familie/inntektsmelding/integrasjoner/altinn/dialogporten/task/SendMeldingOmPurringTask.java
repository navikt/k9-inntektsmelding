package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.task;

import static no.nav.familie.inntektsmelding.forespørsel.tjenester.task.HåndterRekkefølgeAvForespørselTasks.FORESPØRSEL_UUID;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.task.HåndterRekkefølgeAvForespørselTasks;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.DialogportenTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = SendMeldingOmPurringTask.TASK_TYPE)
public class SendMeldingOmPurringTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SendMeldingOmPurringTask.class);
    public static final String TASK_TYPE = "dialogporten.send.purring.melding";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;

    SendMeldingOmPurringTask() {
        // CDI
    }

    @Inject
    public SendMeldingOmPurringTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                    DialogportenTjeneste dialogportenTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        UUID forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));

        ForespørselEntitet forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid " + forespørselUuid));

        if (forespørsel.getStatus() != ForespørselStatus.UNDER_BEHANDLING) {
            LOG.info("Forespørsel med uuid {} har status {}. Sender ikke purring til dialogporten", forespørselUuid, forespørsel.getStatus());
            return;
        }

        if (dialogportenTjeneste.erOpprettetFørProdsetting(forespørsel) && forespørsel.getDialogportenUuid().isEmpty()) {
            LOG.info("Forespørsel med uuid {} er opprettet før Dialogporten ble satt i produksjon. Det finnes derfor ingen forespørsel i Dialogporten. Hopper over oppdatering", forespørselUuid);
            return;
        }

        LOG.info("Sender melding om purring i dialogporten for forespørsel: {}", forespørselUuid);
        dialogportenTjeneste.sendMeldingOmPurring(forespørsel);
    }

    public static ProsessTaskData lagTaskData(UUID forespørselUuid) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(SendMeldingOmPurringTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        HåndterRekkefølgeAvForespørselTasks.setRekkefølgeForForespørselTask(prosessTaskData, forespørselUuid);
        return prosessTaskData;
    }
}
