package no.nav.familie.inntektsmelding.metrikker.bigquery;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Etterfyller BigQuery med godkjente inntektsmeldinger mottatt før løpende publisering startet.
 * Blar på loepenr (eksklusive grenser) og oppretter én {@link PubliserInntektsmeldingStatistikkTask} per inntektsmelding.
 */
@ApplicationScoped
@ProsessTask(value = PubliserHistoriskeInntektsmeldingerStatistikkTask.TASK_TYPE)
public class PubliserHistoriskeInntektsmeldingerStatistikkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PubliserHistoriskeInntektsmeldingerStatistikkTask.class);
    public static final String TASK_TYPE = "bigquery.statistikk.inntektsmelding.historisk";

    static final String LOEPENR_FRA = "loepenr_fra";
    static final String LOEPENR_TIL = "loepenr_til";
    static final String BATCH_SIZE = "batch_size";
    static final String AUTOMATISK_BATCH = "ny_automatisk_batch";

    private InntektsmeldingRepository inntektsmeldingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    PubliserHistoriskeInntektsmeldingerStatistikkTask() {
        // CDI
    }

    @Inject
    public PubliserHistoriskeInntektsmeldingerStatistikkTask(InntektsmeldingRepository inntektsmeldingRepository,
                                                             ProsessTaskTjeneste prosessTaskTjeneste) {
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public static ProsessTaskData opprettTask(long loepenrFra, long loepenrTil, int batchSize, boolean nyAutomatiskBatch) {
        var task = ProsessTaskData.forProsessTask(PubliserHistoriskeInntektsmeldingerStatistikkTask.class);
        task.setProperty(LOEPENR_FRA, String.valueOf(loepenrFra));
        task.setProperty(LOEPENR_TIL, String.valueOf(loepenrTil));
        task.setProperty(BATCH_SIZE, String.valueOf(batchSize));
        task.setProperty(AUTOMATISK_BATCH, String.valueOf(nyAutomatiskBatch));
        return task;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        long loepenrFra = Long.parseLong(prosessTaskData.getPropertyValue(LOEPENR_FRA));
        long loepenrTil = Long.parseLong(prosessTaskData.getPropertyValue(LOEPENR_TIL));
        int batchSize = Integer.parseInt(prosessTaskData.getPropertyValue(BATCH_SIZE));
        boolean nyAutomatiskBatch = Boolean.parseBoolean(prosessTaskData.getPropertyValue(AUTOMATISK_BATCH));

        LOG.info("Henter inntil {} godkjente inntektsmeldinger med loepenr mellom {} og {} (eksklusivt)", batchSize, loepenrFra, loepenrTil);
        List<InntektsmeldingEntitet> inntektsmeldinger = inntektsmeldingRepository.hentGodkjenteInntektsmeldingerMellomLoepenr(loepenrFra,
            loepenrTil, batchSize);

        if (inntektsmeldinger.isEmpty()) {
            LOG.info("Ingen flere inntektsmeldinger å publisere statistikk for. Batch-jobb ferdig.");
            return;
        }

        var tasker = inntektsmeldinger.stream()
            .map(im -> PubliserInntektsmeldingStatistikkTask.opprettTask(im.getId(), im.getForespørsel().getSaksnummer().orElse(null)))
            .toList();
        var gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(tasker);
        prosessTaskTjeneste.lagre(gruppe);
        LOG.info("Opprettet {} tasker for publisering av statistikk", tasker.size());

        if (inntektsmeldinger.size() < batchSize) {
            LOG.info("Siste batch prosessert.");
            return;
        }

        var siste = inntektsmeldinger.getLast();
        if (nyAutomatiskBatch) {
            LOG.info("Oppretter ny batch-task fra loepenr {}", siste.getLoepenr());
            prosessTaskTjeneste.lagre(opprettTask(siste.getLoepenr(), loepenrTil, batchSize, true));
        } else {
            LOG.info("Batch ferdig. Bruk inntektsmelding uuid {} som inntektsmeldingUuidFra i neste kjøring", siste.getUuid());
        }
    }
}
