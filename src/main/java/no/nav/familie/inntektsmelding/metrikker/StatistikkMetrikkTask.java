package no.nav.familie.inntektsmelding.metrikker;

import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.prosesstask.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "statistikk.metrikker", cronExpression = "0 */5 * * * *", maxFailedRuns = 20, firstDelay = 60)
public class StatistikkMetrikkTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StatistikkMetrikkTask.class);
    private static final String PROSESS_TASK_METRIKK_NAVN = "k9_inntektsmelding_prosessTask_feilende";
    private static final AtomicLong GAUGE = REGISTRY.gauge(PROSESS_TASK_METRIKK_NAVN, new AtomicLong(0));

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
        var feilendeProsessTasker = prosessTaskRepository.tellAntallFeilendeProsessTasker();
        LOG.info("Antall feilende prosess tasker: {}", feilendeProsessTasker);
        GAUGE.set(feilendeProsessTasker);
    }
}
