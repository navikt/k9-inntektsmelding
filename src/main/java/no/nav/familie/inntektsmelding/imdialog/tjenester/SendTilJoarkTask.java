package no.nav.familie.inntektsmelding.imdialog.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask(value = "mottaInntektsmelding.oversendJoark")
public class SendTilJoarkTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SendTilJoarkTask.class);

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        // TODO fyll med innhold
        LOG.info("kjører task");
    }
}
