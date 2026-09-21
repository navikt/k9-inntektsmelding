package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.task;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.DialogportenTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = SettDialogTilUtgåttTask.TASK_TYPE)
public class SettDialogTilUtgåttTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SettDialogTilUtgåttTask.class);
    public static final String TASK_TYPE = "dialogporten.utgått.forespørsel";

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

    private DialogportenTjeneste dialogportenTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    SettDialogTilUtgåttTask() {
        // CDI
    }

    @Inject
    public SettDialogTilUtgåttTask(DialogportenTjeneste dialogportenTjeneste, ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.dialogportenTjeneste = dialogportenTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        UUID forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));

        ForespørselEntitet forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid " + forespørselUuid));

        if (dialogportenTjeneste.erOpprettetFørProdsetting(forespørsel) && forespørsel.getDialogportenUuid().isEmpty()) {
            LOG.info("Forespørsel med uuid {} er opprettet før Dialogporten ble satt i produksjon. Det finnes derfor ingen forespørsel i Dialogporten. Hopper over ferdigstilling", forespørselUuid);
            return;
        }

        if (forespørsel.getDialogportenUuid().isEmpty()) {
            LOG.info("Forespørsel med uuid {} mangler dialogportenUuid. Venter 2 sekunder før vi henter forespørsel på nytt, det kan være dialogen nettopp er opprettet i Dialogporten", forespørselUuid);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ble avbrutt under venting på dialogportenUuid for forespørsel " + forespørselUuid, e);
            }

            forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
                .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid " + forespørselUuid));

            if (forespørsel.getDialogportenUuid().isEmpty()) {
                throw new IllegalStateException("Forespørsel med uuid " + forespørselUuid + " mangler fortsatt dialogportenUuid etter ventetid.");
            }
        }

        LOG.info("Setter forespørsel til utgått i dialogporten for forespørsel uuid: {}", forespørselUuid);

        dialogportenTjeneste.settDialogTilUtgått(forespørsel);
    }

    public static ProsessTaskData lagTaskData(UUID forespørselUuid) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(SettDialogTilUtgåttTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        return prosessTaskData;
    }
}
