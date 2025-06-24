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
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.familie.inntektsmelding.utils.K9ObjectMapper;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("forespørsel.opprett")
public class OpprettForespørselTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OpprettForespørselTask.class);

    public static final String YTELSETYPE = "ytelsetype";
    public static final String ORGNR = "orgnr";
    public static final String STP = "skjaeringstidspunkt";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private ObjectMapper objectMapper;

    @Inject
    public OpprettForespørselTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                  K9ObjectMapper k9ObjectMapper) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.objectMapper = k9ObjectMapper.getObjectMapper();
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

        // TODO: Sjekk om det er det har kommet nye etterspurtePerioder som ikke er i eksisterende forespørsel. Oppdater i så fall eksisterende forespørsel med nye perioder.
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
            etterspurtePerioder = objectMapper.readValue(
                prosessTaskData.getPayloadAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, PeriodeDto.class)
            );
            return etterspurtePerioder;
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke deserialisere etterspurtePerioder for ytelse: " + ytelsetype, e);
        }
    }
}
