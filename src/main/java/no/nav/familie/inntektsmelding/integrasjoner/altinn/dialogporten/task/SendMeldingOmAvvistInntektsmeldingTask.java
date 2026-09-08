package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.task;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.DialogportenKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("dialogporten.sendMeldingOmAvvistInntektsmelding")
public class SendMeldingOmAvvistInntektsmeldingTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SendMeldingOmAvvistInntektsmeldingTask.class);

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private DialogportenKlient dialogportenKlient;

    SendMeldingOmAvvistInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public SendMeldingOmAvvistInntektsmeldingTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                                  DialogportenKlient dialogportenKlient) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.dialogportenKlient = dialogportenKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        UUID forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        String feilmelding = prosessTaskData.getPayloadAsString();

        ForespørselEntitet forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid " + forespørselUuid));

        LOG.info("Sender melding om avvist inntektsmelding til dialogporten for forespørsel: {}", forespørselUuid);
        dialogportenKlient.sendMeldingOmAvvistInntektsmelding(forespørsel, feilmelding);
    }

    public static ProsessTaskData lagTaskData(UUID forespørselUuid, String feilmelding) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(SendMeldingOmAvvistInntektsmeldingTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        prosessTaskData.setPayload(feilmelding);
        return prosessTaskData;
    }
}

