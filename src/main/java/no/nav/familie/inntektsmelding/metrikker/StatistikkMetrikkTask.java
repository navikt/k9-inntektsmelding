package no.nav.familie.inntektsmelding.metrikker;

import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.metrikker.bigquery.BigQueryDataset;
import no.nav.familie.inntektsmelding.metrikker.bigquery.tabell.ProsessTaskFeilRecord;
import no.nav.familie.inntektsmelding.metrikker.bigquery.tabell.ProsessTaskFeilTabell;
import no.nav.k9.felles.integrasjon.bigquery.klient.BigQueryKlient;
import no.nav.k9.felles.integrasjon.bigquery.tabell.BigQueryRecord;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEntitet;

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
    private BigQueryKlient bigQueryKlient;

    public StatistikkMetrikkTask() {
        // CDI
    }

    @Inject
    public StatistikkMetrikkTask(ProsessTaskRepository prosessTaskRepository, BigQueryKlient bigQueryKlient) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.bigQueryKlient = bigQueryKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        List<ProsessTaskEntitet> prosessTaskEntitets = prosessTaskRepository.feiledeProsessTasker();
        var feilendeProsessTasker = prosessTaskEntitets.size();
        LOG.info("Antall feilende prosess tasker: {}", feilendeProsessTasker);
        GAUGE.set(feilendeProsessTasker);

        Instant now = Instant.now();
        List<Map<String, ?>> bigQueryRows = prosessTaskEntitets.stream()
            .map(StatistikkMetrikkTask::mapTilRecord)
            .map(it -> ProsessTaskFeilTabell.INSTANCE.getRowMapper(now).apply(it))
            .collect(Collectors.toList());

        bigQueryKlient.tømOgPubliserAtomisk(
            BigQueryDataset.K9_INNTEKTSMELDING_STATISTIKK_DATASET.getDatasetNavn(),
            ProsessTaskFeilTabell.INSTANCE,
            bigQueryRows);
    }

    private static BigQueryRecord mapTilRecord(ProsessTaskEntitet it) {
        var ytelseType = it.getPropertyValue("ytelsetype");
        var saksnummer = it.getPropertyValue("saksnummer");
        return new ProsessTaskFeilRecord(
            ytelseType,
            saksnummer,
            it.getId().toString(),
            it.getTaskType().value(),
            it.getStatus().getDbKode(),
            Optional.ofNullable(it.getSisteKjøring()).map(s -> s.atZone(ZoneId.systemDefault()).toInstant().toString()).orElse(null),
            Optional.ofNullable(it.getBlokkertAvProsessTaskId()).map(Object::toString).orElse(null),
            it.getOpprettetTid().atZone(ZoneId.systemDefault()).toInstant().toString(),
            it.getGruppe(),
            null


        );

    }
}
