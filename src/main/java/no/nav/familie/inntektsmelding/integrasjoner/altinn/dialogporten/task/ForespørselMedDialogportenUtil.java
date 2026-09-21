package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;

@ApplicationScoped
public class ForespørselMedDialogportenUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselMedDialogportenUtil.class);
    private static final long VENTETID_MILLIS = 10000;

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    ForespørselMedDialogportenUtil() {
        // CDI
    }

    @Inject
    public ForespørselMedDialogportenUtil(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    ForespørselEntitet ventOgHentForespørselMedDialogporten(ForespørselEntitet forespørsel) {
        LOG.info("Forespørsel med uuid {} mangler dialogportenUuid. Venter {} sekunder før vi henter forespørsel på nytt, det kan være dialogen nettopp er opprettet i Dialogporten",
            forespørsel.getUuid(), VENTETID_MILLIS / 1000);
        try {
            Thread.sleep(VENTETID_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ble avbrutt under venting på dialogportenUuid for forespørsel " + forespørsel, e);
        }

        forespørselBehandlingTjeneste.refresh(forespørsel);

        if (forespørsel.getDialogportenUuid().isEmpty()) {
            throw new IllegalStateException("Forespørsel med uuid " + forespørsel + " mangler fortsatt dialogportenUuid etter ventetid.");
        }

        return forespørsel;
    }
}
