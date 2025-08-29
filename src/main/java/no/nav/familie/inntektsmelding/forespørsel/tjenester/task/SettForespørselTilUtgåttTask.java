package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("forespørsel.utgått")
public class SettForespørselTilUtgåttTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SettForespørselTilUtgåttTask.class);

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

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
            LOG.warn("Fant ikke forespørsel med uuid {}", forespørselUuid);
            return;
        }

        ForespørselEntitet forespørsel = opt.get();
        if (forespørsel.getStatus() == ForespørselStatus.UTGÅTT) {
            LOG.info("Forespørsel med uuid {} har allerede status utgått", forespørselUuid);
            return;
        }

        boolean skalOppdatereArbeidsgiverNotifikasjon = forespørsel.getStatus() == ForespørselStatus.UNDER_BEHANDLING;
        forespørselBehandlingTjeneste.settForespørselTilUtgått(forespørsel, skalOppdatereArbeidsgiverNotifikasjon);
    }

    public static ProsessTaskData lagSettTilUtgåttTask(UUID forespørselUuid, SaksnummerDto saksnummer) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(SettForespørselTilUtgåttTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        prosessTaskData.setSaksnummer(saksnummer.saksnr());
        return prosessTaskData;
    }
}
