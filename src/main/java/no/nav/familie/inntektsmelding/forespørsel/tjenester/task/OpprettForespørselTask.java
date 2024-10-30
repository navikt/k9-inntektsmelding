package no.nav.familie.inntektsmelding.forespørsel.tjenester.task;

import java.time.LocalDate;
import java.util.List;

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

@ApplicationScoped
@ProsessTask(value = "forespørsel.opprett")
public class OpprettForespørselTask implements ProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettForespørselTask.class);

    public static final String YTELSETYPE = "ytelsetype";
    public static final String AKTØR_ID = "aktørId";
    public static final String FAGSAK_SAKSNUMMER = "fagsakSaksnummer";
    public static final String ORGNR = "orgnr";
    public static final String STP = "skjæringstidspunkt";

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
        AktørIdEntitet aktørId = new AktørIdEntitet(prosessTaskData.getPropertyValue(AKTØR_ID));
        SaksnummerDto fagsakSaksnummer = new SaksnummerDto(prosessTaskData.getPropertyValue(FAGSAK_SAKSNUMMER));
        OrganisasjonsnummerDto organisasjonsnummer = new OrganisasjonsnummerDto(prosessTaskData.getPropertyValue(ORGNR));
        LocalDate skjæringstidspunkt = LocalDate.parse(prosessTaskData.getPropertyValue(STP));

        List<ForespørselEntitet> eksisterendeForespørsler = forespørselBehandlingTjeneste.hentForespørslerForFagsak(fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt);
        if (eksisterendeForespørsler.stream().anyMatch(eksisterende -> !eksisterende.getStatus().equals(ForespørselStatus.UTGÅTT))) {
            log.info("Forespørsel finnes allerede, orgnr: {}, stp: {}, saksnr: {}, ytelse: {}",
                organisasjonsnummer.orgnr(), skjæringstidspunkt, fagsakSaksnummer.saksnr(), ytelsetype);
            return;
        }

        forespørselBehandlingTjeneste.opprettForespørsel(ytelsetype, aktørId, fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt);
    }
}
