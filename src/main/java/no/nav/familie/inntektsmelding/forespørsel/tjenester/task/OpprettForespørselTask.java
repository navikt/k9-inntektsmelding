package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@ProsessTask(value = OpprettForespørselTask.TASKTYPE)
public class OpprettForespørselTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forespørsel.opprett";
    private static final Logger log = LoggerFactory.getLogger(OpprettForespørselTask.class);

    public static final String YTELSETYPE = "ytelsetype";
    public static final String AKTØR_ID = "aktoerId";
    public static final String FAGSAK_SAKSNUMMER = "fagsakSaksnummer";
    public static final String ORGNR = "orgnr";
    public static final String STP = "skjaeringstidspunkt";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public OpprettForespørselTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                  ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    OpprettForespørselTask() {
        // CDI
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Ytelsetype ytelsetype = Ytelsetype.valueOf(prosessTaskData.getPropertyValue(YTELSETYPE));
        AktørIdEntitet aktørId = new AktørIdEntitet(prosessTaskData.getPropertyValue(AKTØR_ID));
        SaksnummerDto fagsakSaksnummer = new SaksnummerDto(prosessTaskData.getPropertyValue(FAGSAK_SAKSNUMMER));
        OrganisasjonsnummerDto organisasjonsnummer = new OrganisasjonsnummerDto(prosessTaskData.getPropertyValue(ORGNR));
        LocalDate skjæringstidspunkt = LocalDate.parse(prosessTaskData.getPropertyValue(STP));

        //TODO lage en query som slipper å søke i parameterteksten
        Optional<ProsessTaskData> blokkerendeTask = prosessTaskTjeneste.finnAlleMedParameterTekst(fagsakSaksnummer.saksnr(), Tid.TIDENES_BEGYNNELSE, LocalDate.now())
            .stream()
            .filter(task -> task.getStatus() == ProsessTaskStatus.KLAR)
            .filter(task -> List.of(OpprettForespørselTask.TASKTYPE, SettForespørselTilUtgåttTask.TASKTYPE).contains(task.getTaskType()))
            .filter(task -> !Objects.equals(task.getGruppe(), prosessTaskData.getGruppe()))
            .filter(task -> task.getOpprettetTid().isBefore(prosessTaskData.getOpprettetTid()))
            .max(Comparator.comparing(ProsessTaskData::getOpprettetTid));

        if (blokkerendeTask.isPresent()) {
            //TODO burde vi lage et bedre rammeverk for å vetoe tasker, slik som i k9-sak?
            log.info("Vetoet av eksisterende task med id {}", blokkerendeTask.get().getId());
            var vetoetTask = ProsessTaskData.forProsessTask(OpprettForespørselTask.class);
            vetoetTask.setProperties(prosessTaskData.getProperties());
            vetoetTask.setBlokkertAvProsessTaskId(blokkerendeTask.get().getId());
            vetoetTask.setStatus(ProsessTaskStatus.VETO);
            prosessTaskTjeneste.lagre(vetoetTask);
            return;
        }

        List<ForespørselEntitet> eksisterendeForespørsler = forespørselBehandlingTjeneste.hentForespørslerForFagsak(fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt);
        if (eksisterendeForespørsler.stream().anyMatch(eksisterende -> !eksisterende.getStatus().equals(ForespørselStatus.UTGÅTT))) {
            log.info("Forespørsel finnes allerede, orgnr: {}, stp: {}, saksnr: {}, ytelse: {}",
                organisasjonsnummer.orgnr(), skjæringstidspunkt, fagsakSaksnummer.saksnr(), ytelsetype);
            return;
        }
        // K9 trenger ikke førsteUttaksdato, setter alltid null her
        forespørselBehandlingTjeneste.opprettForespørsel(ytelsetype, aktørId, fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt,
            null);
    }

    public static ProsessTaskData lagTaskData(Ytelsetype ytelsetype,
                                              AktørIdEntitet aktørId,
                                              SaksnummerDto fagsakSaksnummer,
                                              OrganisasjonsnummerDto organisasjon,
                                              LocalDate skjæringstidspunkt) {
        var taskdata = ProsessTaskData.forProsessTask(OpprettForespørselTask.class);
        taskdata.setProperty(YTELSETYPE, ytelsetype.name());
        taskdata.setProperty(AKTØR_ID, aktørId.getAktørId());
        taskdata.setProperty(FAGSAK_SAKSNUMMER, fagsakSaksnummer.saksnr());
        taskdata.setProperty(ORGNR, organisasjon.orgnr());
        taskdata.setProperty(STP, skjæringstidspunkt.toString());
        return taskdata;
    }
}
