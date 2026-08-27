package no.nav.familie.inntektsmelding.imdialog.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.imapi.inntektsmelding.InntektsmeldingApiMottakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "ferdigstill.etter.nedetid")
public class FerdigstillInntektsmeldingEtterNedetidTask implements ProsessTaskHandler {
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";

    private InntektsmeldingApiMottakTjeneste mottakTjeneste;

    FerdigstillInntektsmeldingEtterNedetidTask() {
        // CDI
    }

    @Inject
    public FerdigstillInntektsmeldingEtterNedetidTask(InntektsmeldingApiMottakTjeneste mottakTjeneste) {
        this.mottakTjeneste = mottakTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long inntektsmeldingId = Long.parseLong(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        mottakTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId);
    }
}
