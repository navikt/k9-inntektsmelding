package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("forespørsel.nyBeskjedOgVarsel")
public class SendNyBeskjedOgVarselTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SendNyBeskjedOgVarselTask.class);
    public static final String FORESPØRSEL_UUID = "forespoerselUUID";

    private final ForespørselRepository forespørselRepository;
    private final ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @Inject
    public SendNyBeskjedOgVarselTask(ForespørselRepository forespørselRepository,
                                     ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselRepository = forespørselRepository;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        UUID uuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        Optional<ForespørselEntitet> forespørsel = forespørselRepository.hentForespørsel(uuid);
        if (forespørsel.isPresent() && forespørsel.get().getStatus() == ForespørselStatus.UNDER_BEHANDLING) {
            LOG.info("Sender ny beskjed for forespørsel: {}", forespørsel);
            forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(forespørsel.get());
        } else {
            LOG.info("Forespørsel med uuid {} er ferdig/utgått eller finnes ikke.", uuid);
        }
    }
}
