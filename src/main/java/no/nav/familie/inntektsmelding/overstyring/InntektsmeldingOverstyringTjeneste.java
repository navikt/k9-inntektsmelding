package no.nav.familie.inntektsmelding.overstyring;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
public class InntektsmeldingOverstyringTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingOverstyringTjeneste.class);

    private InntektsmeldingRepository inntektsmeldingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    InntektsmeldingOverstyringTjeneste() {
        // CDI proxy
    }

    @Inject
    public InntektsmeldingOverstyringTjeneste(InntektsmeldingRepository inntektsmeldingRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void mottaOverstyrtInntektsmelding(SendOverstyrtInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        lagreOgLagJournalførTask(InntektsmeldingOverstyringMapper.mapTilEntitet(mottattInntektsmeldingDto));
    }

    private void lagreOgLagJournalførTask(InntektsmeldingEntitet entitet) {
        opprettTaskForSendTilJoark(inntektsmeldingRepository.lagreInntektsmelding(entitet));
    }

    private void opprettTaskForSendTilJoark(Long imId) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setCallIdFraEksisterende();
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }
}
