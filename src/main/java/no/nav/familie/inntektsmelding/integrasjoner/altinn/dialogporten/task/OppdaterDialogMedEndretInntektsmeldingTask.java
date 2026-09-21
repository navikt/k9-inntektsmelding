package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.task;

import java.util.Optional;
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
@ProsessTask(value = OppdaterDialogMedEndretInntektsmeldingTask.TASK_TYPE)
public class OppdaterDialogMedEndretInntektsmeldingTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterDialogMedEndretInntektsmeldingTask.class);
    public static final String TASK_TYPE = "dialogporten.oppdater.ny.inntektsmelding";

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";
    public static final String INNTEKTSMELDING_UUID = "inntektsmeldingUuid";

    private DialogportenTjeneste dialogportenTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    OppdaterDialogMedEndretInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public OppdaterDialogMedEndretInntektsmeldingTask(DialogportenTjeneste dialogportenTjeneste, ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
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

        Optional<UUID> inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(INNTEKTSMELDING_UUID))
            .map(UUID::fromString);

        LOG.info("Oppdaterer forespørsel med inntektsmelding i dialogporten for forespørsel uuid: {}", forespørselUuid);
        dialogportenTjeneste.oppdaterDialogMedEndretInntektsmelding(forespørsel, inntektsmeldingUuid);
    }

    public static ProsessTaskData lagTaskData(UUID forespørselUuid, Optional<UUID> inntektsmeldingUuid) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(OppdaterDialogMedEndretInntektsmeldingTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        inntektsmeldingUuid.ifPresent(uuid -> prosessTaskData.setProperty(INNTEKTSMELDING_UUID, uuid.toString()));
        return prosessTaskData;
    }
}
