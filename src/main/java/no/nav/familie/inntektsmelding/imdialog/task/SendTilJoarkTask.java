package no.nav.familie.inntektsmelding.imdialog.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.K9DokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.joark.JoarkTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "mottaInntektsmelding.oversendJoark")
public class SendTilJoarkTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SendTilJoarkTask.class);
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";
    public static final String KEY_YTELSE_TYPE = "ytelseType";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste;
    private K9DokgenTjeneste k9DokgenTjeneste;
    private JoarkTjeneste joarkTjeneste;

    SendTilJoarkTask() {
        // CDI
    }

    @Inject
    public SendTilJoarkTask(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                            InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste,
                            K9DokgenTjeneste k9DokgenTjeneste,
                            JoarkTjeneste joarkTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingXMLTjeneste = inntektsmeldingXMLTjeneste;
        this.k9DokgenTjeneste = k9DokgenTjeneste;
        this.joarkTjeneste = joarkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        int inntektsmeldingId = Integer.parseInt(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        String ytelseType = prosessTaskData.getPropertyValue(KEY_YTELSE_TYPE);
        String saksnummer = prosessTaskData.getSaksnummer();

        LOG.info("Starter task for oversending til joark for ytelse {} saksnummer {}", ytelseType, saksnummer);

        InntektsmeldingEntitet inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        String xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmelding);
        byte[] pdf = k9DokgenTjeneste.mapDataOgGenererPdf(inntektsmelding);

        joarkTjeneste.journalførInntektsmelding(xml, inntektsmelding, pdf, saksnummer);
        LOG.info("Sluttfører task oversendJoark");
    }
}
