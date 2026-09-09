package no.nav.familie.inntektsmelding.imdialog.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.imapi.inntektsmelding.InntektsmeldingApiMottakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = KontrollerInntektsmeldingEtterNedetidTask.TASK_TYPE)
public class KontrollerInntektsmeldingEtterNedetidTask implements ProsessTaskHandler {
    public static final String TASK_TYPE = "inntektsmelding.kontroller.etter.nedetid";
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";

    private InntektsmeldingApiMottakTjeneste mottakTjeneste;

    KontrollerInntektsmeldingEtterNedetidTask() {
        // CDI
    }

    @Inject
    public KontrollerInntektsmeldingEtterNedetidTask(InntektsmeldingApiMottakTjeneste mottakTjeneste) {
        this.mottakTjeneste = mottakTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long inntektsmeldingId = Long.parseLong(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        mottakTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId);
    }
}
