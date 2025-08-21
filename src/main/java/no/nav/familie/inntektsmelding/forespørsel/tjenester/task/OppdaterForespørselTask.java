package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@ProsessTask("forespørsel.oppdater")
public class OppdaterForespørselTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterForespørselTask.class);
    private static final ObjectMapper OBJECT_MAPPER = DefaultJsonMapper.getObjectMapper();

    public static final String FORESPØRSEL_UUID = "forespoersel_uuid";
    public static final String YTELSETYPE = "ytelsetype";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    OppdaterForespørselTask() {
        // CDI
    }

    @Inject
    public OppdaterForespørselTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Ytelsetype ytelseType = Ytelsetype.valueOf(prosessTaskData.getPropertyValue(YTELSETYPE));

        if (ytelseType != Ytelsetype.OMSORGSPENGER) {
            throw new IllegalStateException("Støtter kun oppdatering av forespørsel for OMSORGSPENGER, fikk: " + ytelseType);
        }

        UUID forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        List<PeriodeDto> etterspurtePerioder = hentEtterspurtePerioder(prosessTaskData);
        forespørselBehandlingTjeneste.oppdaterForespørselMedNyeEtterspurtePerioder(forespørselUuid, etterspurtePerioder);
    }

    private List<PeriodeDto> hentEtterspurtePerioder(ProsessTaskData prosessTaskData) {
        List<PeriodeDto> etterspurtePerioder;

        try {
            etterspurtePerioder = OBJECT_MAPPER.readValue(prosessTaskData.getPayloadAsString(), OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, PeriodeDto.class));
            return etterspurtePerioder;
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke deserialisere etterspurtePerioder", e);
        }
    }

    public static ProsessTaskData lagOppdaterTaskData(UUID forespørselUuid,
                                                      Ytelsetype ytelseType,
                                                      List<PeriodeDto> etterspurtePerioder) {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(OppdaterForespørselTask.class);
        taskData.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        taskData.setProperty(YTELSETYPE, ytelseType.name());

        if (etterspurtePerioder != null) {
            try {
                taskData.setPayload(OBJECT_MAPPER.writeValueAsString(etterspurtePerioder));
            } catch (Exception e) {
                throw new RuntimeException("Kunne ikke serialisere etterspurtePerioder for ytelse: " + ytelseType, e);
            }
        }
        return taskData;
    }
}
