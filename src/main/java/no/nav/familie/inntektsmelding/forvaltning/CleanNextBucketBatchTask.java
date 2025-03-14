package no.nav.familie.inntektsmelding.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "partition.cleanBucket", prioritet = 3, cronExpression = "0 0 0 1 * *", maxFailedRuns = 1)
public class CleanNextBucketBatchTask implements ProsessTaskHandler {

    private final ProsessTaskTjeneste taskTjeneste;

    @Inject
    public CleanNextBucketBatchTask(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        taskTjeneste.tømNestePartisjon();
    }
}
