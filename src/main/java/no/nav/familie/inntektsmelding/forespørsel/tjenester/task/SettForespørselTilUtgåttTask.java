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
@ProsessTask(value = "forespørsel.utgått")
public class SettForespørselTilUtgåttTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(SettForespørselTilUtgåttTask.class);

    public static final String FORESPØRSEL_UUID = "forespørselUuid";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @Inject
    public SettForespørselTilUtgåttTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    SettForespørselTilUtgåttTask() {
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
        if (forespørsel.getStatus() != ForespørselStatus.UNDER_BEHANDLING) {
            log.info("Forespørsel med uuid {} kan ikke settes til utgått, status er {}", forespørselUuid, forespørsel.getStatus());
            return;
        }

        forespørselBehandlingTjeneste.settForespørselTilUtgått(forespørsel);
    }
}
