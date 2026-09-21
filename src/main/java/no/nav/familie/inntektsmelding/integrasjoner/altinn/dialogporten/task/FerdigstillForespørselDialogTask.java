package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.task;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.DialogportenTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = FerdigstillForespørselDialogTask.TASK_TYPE)
public class FerdigstillForespørselDialogTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FerdigstillForespørselDialogTask.class);
    public static final String TASK_TYPE = "dialogporten.ferdigstill.forespørsel";

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";
    public static final String INNTEKTSMELDING_UUID = "inntektsmeldingUuid";
    public static final String LUKKE_ÅRSAK = "lukkeAarsak";

    private DialogportenTjeneste dialogportenTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private ForespørselMedDialogportenUtil forespørselMedDialogportenUtil;

    FerdigstillForespørselDialogTask() {
        // CDI
    }

    @Inject
    public FerdigstillForespørselDialogTask(DialogportenTjeneste dialogportenTjeneste,
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

        LukkeÅrsak lukkeÅrsak = LukkeÅrsak.valueOf(prosessTaskData.getPropertyValue(LUKKE_ÅRSAK));

        Optional<UUID> inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(INNTEKTSMELDING_UUID))
            .map(UUID::fromString);

        LOG.info("Ferdigstiller forespørsel i dialogporten for forespørsel uuid: {}", forespørselUuid);
        dialogportenTjeneste.ferdigstillDialog(
            forespørsel,
            inntektsmeldingUuid,
            lukkeÅrsak
        );
    }

    public static ProsessTaskData lagTaskData(UUID forespørselUuid, Optional<UUID> inntektsmeldingUuid, LukkeÅrsak lukkeÅrsak) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(FerdigstillForespørselDialogTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        prosessTaskData.setProperty(LUKKE_ÅRSAK, lukkeÅrsak.name());
        inntektsmeldingUuid.ifPresent(uuid -> prosessTaskData.setProperty(INNTEKTSMELDING_UUID, uuid.toString()));
        return prosessTaskData;
    }
}
