package no.nav.familie.inntektsmelding.metrikker;

import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.prosesstask.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "statistikk.metrikker", cronExpression = "0 */5 * * * *", maxFailedRuns = 20, firstDelay = 60)
public class StatistikkMetrikkTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StatistikkMetrikkTask.class);
    private static final String PROSESS_TASK_METRIKK_NAVN = "k9-inntektsmelding.prosessTask.feilende";
    private ProsessTaskRepository prosessTaskRepository;

    public StatistikkMetrikkTask() {
        // CDI
    }

    @Inject
    public StatistikkMetrikkTask(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        long startTime = System.nanoTime();

        var feilendeProsessTasker = prosessTaskRepository.tellAntallFeilendeProsessTasker();
        REGISTRY.gauge(PROSESS_TASK_METRIKK_NAVN, new AtomicLong(feilendeProsessTasker));

        var varighet = System.nanoTime() - startTime;
        LOG.info("Henting av statistikk tok: {}", varighet);
    }
}
