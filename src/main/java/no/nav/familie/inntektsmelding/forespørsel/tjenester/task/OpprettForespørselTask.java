package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@ProsessTask("forespørsel.opprett")
public class OpprettForespørselTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettForespørselTask.class);
    private static final ObjectMapper OBJECT_MAPPER = DefaultJsonMapper.getObjectMapper();

    public static final String YTELSETYPE = "ytelsetype";
    public static final String ORGNR = "orgnr";
    public static final String STP = "skjaeringstidspunkt";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @Inject
    public OpprettForespørselTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    OpprettForespørselTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Ytelsetype ytelsetype = Ytelsetype.valueOf(prosessTaskData.getPropertyValue(YTELSETYPE));
        AktørIdEntitet aktørId = new AktørIdEntitet(prosessTaskData.getAktørId());
        SaksnummerDto saksnummer = new SaksnummerDto(prosessTaskData.getSaksnummer());
        OrganisasjonsnummerDto organisasjonsnummer = new OrganisasjonsnummerDto(prosessTaskData.getPropertyValue(ORGNR));
        LocalDate skjæringstidspunkt = LocalDate.parse(prosessTaskData.getPropertyValue(STP));
        List<PeriodeDto> etterspurtePerioder = hentEtterspurtePerioder(prosessTaskData, ytelsetype);

        List<ForespørselEntitet> eksisterendeForespørsler = forespørselBehandlingTjeneste.hentForespørslerForFagsak(saksnummer, organisasjonsnummer, skjæringstidspunkt);

        if (eksisterendeForespørsler.stream().anyMatch(eksisterende -> !eksisterende.getStatus().equals(ForespørselStatus.UTGÅTT))) {
            LOG.info("Forespørsel finnes allerede, orgnr: {}, stp: {}, saksnr: {}, ytelse: {}",
                organisasjonsnummer.orgnr(), skjæringstidspunkt, saksnummer.saksnr(), ytelsetype);
            return;
        }

        // K9 trenger ikke førsteUttaksdato, setter alltid null her
        forespørselBehandlingTjeneste.opprettForespørsel(ytelsetype, aktørId, saksnummer, organisasjonsnummer, skjæringstidspunkt, null, etterspurtePerioder);
        MetrikkerTjeneste.loggForespørselOpprettet(ytelsetype);
    }

    private List<PeriodeDto> hentEtterspurtePerioder(ProsessTaskData prosessTaskData, Ytelsetype ytelsetype) {
        List<PeriodeDto> etterspurtePerioder;
        if (ytelsetype != Ytelsetype.OMSORGSPENGER) {
            return null;
        }

        try {
            etterspurtePerioder = OBJECT_MAPPER.readValue(
                prosessTaskData.getPayloadAsString(),
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, PeriodeDto.class)
            );
            return etterspurtePerioder;
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke deserialisere etterspurtePerioder for ytelse: " + ytelsetype, e);
        }
    }

    public static ProsessTaskData lagOpprettForespørselTaskData(Ytelsetype ytelsetype,
                                                                AktørIdEntitet aktørId,
                                                                SaksnummerDto saksnummer,
                                                                OppdaterForespørselDto forespørselDto) {
        var taskdata = ProsessTaskData.forProsessTask(OpprettForespørselTask.class);
        taskdata.setProperty(OpprettForespørselTask.YTELSETYPE, ytelsetype.name());
        taskdata.setAktørId(aktørId.getAktørId());
        taskdata.setSaksnummer(saksnummer.saksnr());
        taskdata.setProperty(OpprettForespørselTask.ORGNR, forespørselDto.orgnr().orgnr());
        taskdata.setProperty(OpprettForespørselTask.STP, forespørselDto.skjæringstidspunkt().toString());
        if (forespørselDto.etterspurtePerioder() != null) {
            try {
                taskdata.setPayload(OBJECT_MAPPER.writeValueAsString(forespørselDto.etterspurtePerioder()));
            } catch (Exception e) {
                LOG.error("Kunne ikke serialisere etterspurtePerioder til JSON", e);
                throw new RuntimeException("Kunne ikke serialisere etterspurtePerioder", e);
            }
        }
        return taskdata;
    }
}
