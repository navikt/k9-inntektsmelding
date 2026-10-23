package no.nav.familie.inntektsmelding.metrikker.bigquery;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.bigquery.InsertAllRequest;

import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.k9.felles.integrasjon.bigquery.klient.BigQueryKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = PubliserInntektsmeldingStatistikkTask.TASK_TYPE)
public class PubliserInntektsmeldingStatistikkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PubliserInntektsmeldingStatistikkTask.class);
    public static final String TASK_TYPE = "bigquery.statistikk.inntektsmelding";
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";
    static final String DATASETT = "k9_inntektsmelding_statistikk_dataset";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BigQueryKlient bigQueryKlient;
    private boolean bigQueryEnabled;

    PubliserInntektsmeldingStatistikkTask() {
        // CDI
    }

    @Inject
    public PubliserInntektsmeldingStatistikkTask(InntektsmeldingTjeneste inntektsmeldingTjeneste, BigQueryKlient bigQueryKlient) {
        this(inntektsmeldingTjeneste, bigQueryKlient, Environment.current().getProperty("BIGQUERY_ENABLED", boolean.class, false));
    }

    PubliserInntektsmeldingStatistikkTask(InntektsmeldingTjeneste inntektsmeldingTjeneste, BigQueryKlient bigQueryKlient, boolean bigQueryEnabled) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.bigQueryKlient = bigQueryKlient;
        this.bigQueryEnabled = bigQueryEnabled;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (!bigQueryEnabled) {
            LOG.info("BigQuery er ikke aktivert, publiserer ikke statistikk");
            return;
        }
        long inntektsmeldingId = Long.parseLong(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        var record = InntektsmeldingMottattRecord.fra(inntektsmelding);
        var tabell = InntektsmeldingMottattTabell.INSTANCE;
        var rad = tabell.getRowMapper(null).apply(record);

        bigQueryKlient.hentEllerOpprettTabell(DATASETT, tabell);
        bigQueryKlient.publiser(DATASETT, tabell, List.of(InsertAllRequest.RowToInsert.of(record.uuid().toString(), rad)));
        LOG.info("Publiserte statistikk til BigQuery for inntektsmelding med uuid: {} og løpenr: {}", inntektsmelding.getUuid(), inntektsmelding.getId());
    }

    public static ProsessTaskData opprettTask(long inntektsmeldingId, String saksnummer) {
        var task = ProsessTaskData.forProsessTask(PubliserInntektsmeldingStatistikkTask.class);
        if (saksnummer != null) {
            task.setSaksnummer(saksnummer);
        }
        task.setProperty(PubliserInntektsmeldingStatistikkTask.KEY_INNTEKTSMELDING_ID, String.valueOf(inntektsmeldingId));
        return task;
    }
}
