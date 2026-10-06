package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import static no.nav.familie.inntektsmelding.forespørsel.tjenester.task.HåndterRekkefølgeAvForespørselTasks.FORESPØRSEL_UUID;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("forespørsel.ferdigstill")
public class FerdigstillForespørselTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FerdigstillForespørselTask.class);

    public static final String INNTEKTSMELDING_UUID = "inntektsmeldingUuid";
    public static final String LUKKE_ÅRSAK = "lukkeAarsak";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;

    @Inject
    public FerdigstillForespørselTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                      InntektsmeldingRepository inntektsmeldingRepository) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    FerdigstillForespørselTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        UUID forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        Optional<ForespørselEntitet> forespørselOptional = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid);

        if (forespørselOptional.isEmpty()) {
            LOG.warn("Fant ikke forespørsel med uuid {} ved ferdigstilling", forespørselUuid);
            return;
        }

        ForespørselEntitet forespørsel = forespørselOptional.get();
        if (forespørsel.getStatus() == ForespørselStatus.FERDIG) {
            LOG.info("Forespørsel med uuid {} er allerede ferdigstilt", forespørselUuid);
            return;
        }

        LukkeÅrsak lukkeÅrsak = LukkeÅrsak.valueOf(prosessTaskData.getPropertyValue(LUKKE_ÅRSAK));
        Optional<InntektsmeldingEntitet> inntektsmelding = Optional.ofNullable(prosessTaskData.getPropertyValue(INNTEKTSMELDING_UUID))
            .map(UUID::fromString)
            .map(uuid -> inntektsmeldingRepository.hentInntektsmeldingForUuid(uuid)
                .orElseThrow(() -> new IllegalStateException("Finner ikke inntektsmelding med uuid " + uuid)));

        ForespørselEntitet lukketForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            forespørsel.getAktørId(),
            new OrganisasjonsnummerDto(forespørsel.getOrganisasjonsnummer()),
            lukkeÅrsak,
            inntektsmelding);

        if (lukkeÅrsak == LukkeÅrsak.EKSTERN_INNSENDING) {
            MetrikkerTjeneste.loggForespørselLukkEkstern(lukketForespørsel);
        } else if (lukkeÅrsak == LukkeÅrsak.ORDINÆR_INNSENDING) {
            MetrikkerTjeneste.loggForespørselLukkIntern(lukketForespørsel);
        }
    }

    public static ProsessTaskData lagTaskData(ForespørselEntitet forespørsel, Optional<UUID> inntektsmeldingUuid, LukkeÅrsak lukkeÅrsak) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(FerdigstillForespørselTask.class);
        prosessTaskData.setProperty(FORESPØRSEL_UUID, forespørsel.getUuid().toString());
        prosessTaskData.setProperty(LUKKE_ÅRSAK, lukkeÅrsak.name());
        inntektsmeldingUuid.ifPresent(uuid -> prosessTaskData.setProperty(INNTEKTSMELDING_UUID, uuid.toString()));
        forespørsel.getSaksnummer().ifPresent(prosessTaskData::setSaksnummer);
        HåndterRekkefølgeAvForespørselTasks.setRekkefølgeForForespørselTask(prosessTaskData, forespørsel.getUuid());
        return prosessTaskData;
    }
}
