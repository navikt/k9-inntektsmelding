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
    private ForespørselMedDialogportenUtil forespørselMedDialogportenUtil;

    SettDialogTilUtgåttTask() {
        // CDI
    }

    @Inject
    public SettDialogTilUtgåttTask(DialogportenTjeneste dialogportenTjeneste,
                                   ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                   ForespørselMedDialogportenUtil forespørselMedDialogportenUtil) {
        this.dialogportenTjeneste = dialogportenTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.forespørselMedDialogportenUtil = forespørselMedDialogportenUtil;
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
            forespørsel = forespørselMedDialogportenUtil.ventOgHentForespørselMedDialogporten(forespørsel);
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
