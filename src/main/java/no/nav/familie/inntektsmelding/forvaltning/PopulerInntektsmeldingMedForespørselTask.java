package no.nav.familie.inntektsmelding.forvaltning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("forvaltning.populerInntektsmeldingMedForespoersel")
public class PopulerInntektsmeldingMedForespørselTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PopulerInntektsmeldingMedForespørselTask.class);

    public static final String FORESPØRSEL_UUID = "forespoerselUuid";

    private ForespørselRepository forespoerselRepository;
    private InntektsmeldingRepository inntektsmeldingRepository;

    @Inject
    public PopulerInntektsmeldingMedForespørselTask(ForespørselRepository forespoerselRepository,
                                                    InntektsmeldingRepository inntektsmeldingRepository) {
        this.forespoerselRepository = forespoerselRepository;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    PopulerInntektsmeldingMedForespørselTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String forespørselUuid = prosessTaskData.getPropertyValue(FORESPØRSEL_UUID);
        Optional<ForespørselEntitet> opt = forespoerselRepository.hentForespørsel(UUID.fromString(forespørselUuid));

        if (opt.isEmpty()) {
            LOG.warn("Fant ikke forespørsel med uuid {}", forespørselUuid);
            return;
        }
        ForespørselEntitet forespørsel = opt.get();

        List<InntektsmeldingEntitet> inntektsmeldinger = inntektsmeldingRepository.hentInntektsmeldinger(forespørsel.getAktørId(),
            forespørsel.getOrganisasjonsnummer(), forespørsel.getSkjæringstidspunkt(), forespørsel.getYtelseType());

        if (inntektsmeldinger == null || inntektsmeldinger.isEmpty()) {
            LOG.info("Fant ingen inntektsmeldinger for forespørsel med uuid {}", forespørselUuid);
            return;
        }

        for (InntektsmeldingEntitet inntektsmelding : inntektsmeldinger) {
            if (inntektsmelding.getForespørsel() == null) {
                inntektsmelding.setForespørsel(forespørsel);
                inntektsmeldingRepository.oppdaterInntektsmelding(inntektsmelding);
                LOG.info("Oppdaterte inntektsmelding med forespørsel");
            }

        }

    }
}
