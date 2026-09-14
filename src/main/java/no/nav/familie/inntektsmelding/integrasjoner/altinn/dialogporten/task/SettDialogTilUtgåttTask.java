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

        LOG.info("Oppretter forespørsel i dialogporten for forespørsel uuid: {}", forespørselUuid);

        if (forespørsel.getDialogportenUuid().isEmpty()) {
            throw new IllegalStateException("Forespørsel med uuid " + forespørselUuid + " har ikke dialogportenUuid satt");
        }

        dialogportenTjeneste.settDialogTilUtgått(
            forespørsel.getDialogportenUuid().get(),
            forespørsel.getAktørId()
        );
    }

    public static ProsessTaskData lagTaskData(UUID forespørselUuid) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(SettDialogTilUtgåttTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        return prosessTaskData;
    }
}
