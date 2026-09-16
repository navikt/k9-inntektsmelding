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
@ProsessTask(value = SendMeldingOmAvvistInntektsmeldingTask.TASK_TYPE)
public class SendMeldingOmAvvistInntektsmeldingTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SendMeldingOmAvvistInntektsmeldingTask.class);
    public static final String TASK_TYPE = "dialogporten.send.avvist.melding";

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;

    SendMeldingOmAvvistInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public SendMeldingOmAvvistInntektsmeldingTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                                  DialogportenTjeneste dialogportenTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        UUID forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        String feilmelding = prosessTaskData.getPayloadAsString();

        ForespørselEntitet forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid " + forespørselUuid));

        if (dialogportenTjeneste.erOpprettetFørProdsetting(forespørsel) && forespørsel.getDialogportenUuid().isEmpty()) {
            LOG.info("Forespørsel med uuid {} er opprettet før Dialogporten ble satt i produksjon. Det finnes derfor ingen forespørsel i Dialogporten. Hopper over ferdigstilling", forespørselUuid);
            return;
        }

        LOG.info("Sender melding om avvist inntektsmelding til dialogporten for forespørsel: {}", forespørselUuid);
        dialogportenTjeneste.sendMeldingOmAvvistInntektsmelding(forespørsel, feilmelding);
    }

    public static ProsessTaskData lagTaskData(UUID forespørselUuid, String feilmelding) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(SendMeldingOmAvvistInntektsmeldingTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        prosessTaskData.setPayload(feilmelding);
        return prosessTaskData;
    }
}

