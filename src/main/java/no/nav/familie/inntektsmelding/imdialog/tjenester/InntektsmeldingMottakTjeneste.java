package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class InntektsmeldingMottakTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingMottakTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    InntektsmeldingMottakTjeneste() {
        // CDI
    }

    @Inject
    public InntektsmeldingMottakTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         InntektsmeldingRepository inntektsmeldingRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public InntektsmeldingResponseDto mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(mottattInntektsmeldingDto.foresporselUuid())
            .orElseThrow(this::manglerForespørselFeil);

        if (ForespørselStatus.UTGÅTT.equals(forespørselEntitet.getStatus())) {
            LOG.error("Mottok inntektsmelding på utgått forespørsel, uuid: {}", mottattInntektsmeldingDto.foresporselUuid());
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }

        var aktorId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto, forespørselEntitet);
        var imId = lagreOgLagJournalførTask(entitet, forespørselEntitet);

        List<FraværsPeriodeEntitet> omsorgspengerFraværsPerioder = entitet.getOmsorgspenger() != null
            ? entitet.getOmsorgspenger().getFraværsPerioder()
            : List.of();

        List<DelvisFraværsPeriodeEntitet> omsorgspengerDelvisFraværsPerioder = entitet.getOmsorgspenger() != null
            ? entitet.getOmsorgspenger().getDelvisFraværsPerioder()
            : List.of();

        var lukketForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktorId, orgnummer,
            LukkeÅrsak.ORDINÆR_INNSENDING, omsorgspengerFraværsPerioder, omsorgspengerDelvisFraværsPerioder);

        var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

        // Metrikker i prometheus
        MetrikkerTjeneste.loggForespørselLukkIntern(lukketForespørsel);
        MetrikkerTjeneste.loggInnsendtInntektsmelding(imEntitet);

        return InntektsmeldingMapper.mapFraEntitet(imEntitet, mottattInntektsmeldingDto.foresporselUuid());
    }

    public InntektsmeldingResponseDto mottaInntektsmeldingForOmsorgspengerRefusjon(SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        var ytelseType = KodeverkMapper.mapYtelsetype(sendInntektsmeldingRequestDto.ytelse());
        if (ytelseType != Ytelsetype.OMSORGSPENGER) {
            throw new IllegalArgumentException("Feil ytelseType for inntektsmelding for omsorgspenger refusjon, ytelsetype var " + ytelseType);
        }

        var aktørId = new AktørIdEntitet(sendInntektsmeldingRequestDto.aktorId().id());
        var organisasjonsnummer = new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident());

        var forespørselUuid = forespørselBehandlingTjeneste.opprettForespørselForOmsorgspengerRefusjonIm(aktørId, organisasjonsnummer, sendInntektsmeldingRequestDto.startdato());
        var forespørselEnitet = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(this::manglerForespørselFeil);

        var imEnitet = InntektsmeldingMapper.mapTilEntitet(sendInntektsmeldingRequestDto, forespørselEnitet);
        var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);

        var fraværsPerioder = imEnitet.getOmsorgspenger().getFraværsPerioder();
        var delvisFraværsPerioder = imEnitet.getOmsorgspenger().getDelvisFraværsPerioder();

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid, aktørId, organisasjonsnummer, LukkeÅrsak.ORDINÆR_INNSENDING, fraværsPerioder, delvisFraværsPerioder);

        var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

        // Metrikker i prometheus
        MetrikkerTjeneste.logginnsendtImOmsorgspengerRefusjon(imEntitet);

        return InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselUuid);
    }

    private InntektsmeldingResponseDto mottaArbeidsgiverInitiertNyansattInntektsmelding(SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        var finnesForespørselFraFør = sendInntektsmeldingRequestDto.foresporselUuid() != null;
        if (finnesForespørselFraFør) {
            // Endring av allerede innsendt inntektsmelding skal følge vanlig flyt
            return mottaInntektsmelding(sendInntektsmeldingRequestDto);
        }

        // Ny inntekstmelding for nyansatt uten forespørsel. Må opprette forespørsel og ferdigstille den slik at det blir riktig i oversikten på Min side - Arbeidsgiver
        var aktørId = new AktørIdEntitet(sendInntektsmeldingRequestDto.aktorId().id());
        var organisasjonsnummer = new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident());
        var ytelseType = KodeverkMapper.mapYtelsetype(sendInntektsmeldingRequestDto.ytelse());

        var forespørselUuid = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertInntektsmelding(aktørId, organisasjonsnummer, sendInntektsmeldingRequestDto.startdato(), ytelseType);
        var forespørselEnitet = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(this::manglerForespørselFeil);

        var inntektsmeldingEntitet = InntektsmeldingMapper.mapTilEntitet(sendInntektsmeldingRequestDto, forespørselEnitet);
        var inntektsmeldingId = lagreOgLagJournalførTask(inntektsmeldingEntitet, forespørselEnitet);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid, aktørId, organisasjonsnummer, LukkeÅrsak.ORDINÆR_INNSENDING);
        var opprettetInntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);

        return InntektsmeldingMapper.mapFraEntitet(opprettetInntektsmeldingEntitet, forespørselUuid);
    }

    private Long lagreOgLagJournalførTask(InntektsmeldingEntitet inntektsmeldingEntitet, ForespørselEntitet forespørsel) {
        var ytelseType = inntektsmeldingEntitet.getYtelsetype();
        LOG.info("Lagrer inntektsmelding for for ytelse {} og fagsak saksnummer {}", ytelseType, forespørsel.getSaksnummer().orElse(null));

        var imId = inntektsmeldingRepository.lagreInntektsmelding(inntektsmeldingEntitet);
        opprettTaskForSendTilJoark(imId, ytelseType, forespørsel);
        return imId;
    }

    private void opprettTaskForSendTilJoark(Long imId, Ytelsetype ytelsetype, ForespørselEntitet forespørsel) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);

        forespørsel.getSaksnummer().ifPresent(task::setSaksnummer);
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setProperty(SendTilJoarkTask.KEY_YTELSE_TYPE, ytelsetype.toString());
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }

    private TekniskException manglerForespørselFeil() {
        return new TekniskException("K9INNTEKTSMELDIMG_FORESPØRSEL_1", "Mangler forespørsel entitet");
    }
}
