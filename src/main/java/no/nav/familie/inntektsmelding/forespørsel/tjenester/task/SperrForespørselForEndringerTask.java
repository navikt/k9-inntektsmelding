package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = SperrForespørselForEndringerTask.TASKTYPE)
public class SperrForespørselForEndringerTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forespørsel.sperrForEndringer";
    private static final Logger log = LoggerFactory.getLogger(SperrForespørselForEndringerTask.class);

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @Inject
    public SperrForespørselForEndringerTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    SperrForespørselForEndringerTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String forespørselUuid = prosessTaskData.getPropertyValue(FORESPØRSEL_UUID);
        Optional<ForespørselEntitet> opt = forespørselBehandlingTjeneste.hentForespørsel(UUID.fromString(forespørselUuid));

        if (opt.isEmpty()) {
            log.warn("Fant ikke forespørsel med uuid {}", forespørselUuid);
            return;
        }

        ForespørselEntitet forespørsel = opt.get();
        if (forespørsel.getStatus() != ForespørselStatus.FERDIG) {
            log.info("Forespørsel med uuid {} kan ikke settes sperres for endringer, status er {}", forespørselUuid, forespørsel.getStatus());
            return;
        }

        forespørselBehandlingTjeneste.settForespørselTilUtgått(forespørsel, false);
    }
}
