package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SlåOppArbeidstakerResponseDto;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.K9DokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class InntektsmeldingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;
    private K9DokgenTjeneste k9DokgenTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    InntektsmeldingTjeneste() {
    }

    @Inject
    public InntektsmeldingTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                   InntektsmeldingRepository inntektsmeldingRepository,
                                   PersonTjeneste personTjeneste,
                                   OrganisasjonTjeneste organisasjonTjeneste,
                                   InntektTjeneste inntektTjeneste,
                                   K9DokgenTjeneste k9DokgenTjeneste,
                                   ProsessTaskTjeneste prosessTaskTjeneste,
                                   ArbeidstakerTjeneste arbeidstakerTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.k9DokgenTjeneste = k9DokgenTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
    }

    public InntektsmeldingResponseDto mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(mottattInntektsmeldingDto.foresporselUuid())
            .orElseThrow(() -> new IllegalStateException("Mangler forespørsel entitet"));

        if (ForespørselStatus.UTGÅTT.equals(forespørselEntitet.getStatus())) {
            throw new IllegalStateException("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
        }

        var aktorId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto, forespørselEntitet);
        var imId = lagreOgLagJournalførTask(entitet, forespørselEntitet);
        var lukketForespørsel = forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktorId, orgnummer,
            LukkeÅrsak.ORDINÆR_INNSENDING, List.of(), List.of());

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

        var forespørselUuid = forespørselBehandlingTjeneste.opprettForespørselForOmsorgspengerRefusjonIm(
            aktørId,
            organisasjonsnummer,
            sendInntektsmeldingRequestDto.startdato());

        var forespørselEnitet = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Mangler forespørsel entitet"));

        var imEnitet = InntektsmeldingMapper.mapTilEntitet(sendInntektsmeldingRequestDto, forespørselEnitet);
        var imId = lagreOgLagJournalførTask(imEnitet, forespørselEnitet);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid, aktørId, organisasjonsnummer,
            LukkeÅrsak.ORDINÆR_INNSENDING, imEnitet.getOmsorgspenger().getFraværsPerioder(), imEnitet.getOmsorgspenger().getDelvisFraværsPerioder());

        var imEntitet = inntektsmeldingRepository.hentInntektsmelding(imId);

        // Metrikker i prometheus
        MetrikkerTjeneste.logginnsendtImOmsorgspengerRefusjon(imEntitet);

        return InntektsmeldingMapper.mapFraEntitet(imEntitet, forespørselUuid);
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

    public InntektsmeldingDialogDto lagDialogDto(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException(
                "Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));
        var personDto = lagPersonDto(forespørsel.getAktørId(), forespørsel.getYtelseType());
        var organisasjonDto = lagOrganisasjonDto(forespørsel.getOrganisasjonsnummer());
        var innmelderDto = lagInnmelderDto(forespørsel.getYtelseType());
        var inntektDtoer = lagInntekterDto(forespørsel.getUuid(),
            forespørsel.getAktørId(),
            forespørsel.getSkjæringstidspunkt(),
            forespørsel.getOrganisasjonsnummer());
        return new InntektsmeldingDialogDto(personDto,
            organisasjonDto,
            innmelderDto,
            inntektDtoer,
            forespørsel.getSkjæringstidspunkt(),
            KodeverkMapper.mapYtelsetype(forespørsel.getYtelseType()),
            forespørsel.getUuid(),
            KodeverkMapper.mapForespørselStatus(forespørsel.getStatus()),
            forespørsel.getFørsteUttaksdato().orElseGet(forespørsel::getSkjæringstidspunkt));
    }

    public InntektsmeldingDialogDto lagArbeidsgiverinitiertDialogDto(PersonIdent fødselsnummer,
                                                                     Ytelsetype ytelsetype,
                                                                     LocalDate førsteFraværsdag,
                                                                     OrganisasjonsnummerDto organisasjonsnummer) {
        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype);

        var eksisterendeForepørsler = forespørselBehandlingTjeneste.finnForespørsler(personInfo.aktørId(), ytelsetype, organisasjonsnummer.orgnr());
        var forespørslerSomMatcherFraværsdag = eksisterendeForepørsler.stream()
            .filter(f -> førsteFraværsdag.equals(f.getFørsteUttaksdato().orElse(f.getSkjæringstidspunkt()))) // TODO: sjekk for et større intervall etterhvert
            .toList();

        if (!forespørslerSomMatcherFraværsdag.isEmpty()) {
            var forespørsel = forespørslerSomMatcherFraværsdag.getFirst();
            return lagDialogDto(forespørsel.getUuid());
        }

        var personDto = new InntektsmeldingDialogDto.PersonInfoResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
        var organisasjonDto = lagOrganisasjonDto(organisasjonsnummer.orgnr());
        var innmelderDto = lagInnmelderDto(ytelsetype);
        var inntektDtoer = lagInntekterDto(null, personInfo.aktørId(), førsteFraværsdag, organisasjonsnummer.orgnr());
        return new InntektsmeldingDialogDto(personDto,
            organisasjonDto,
            innmelderDto,
            inntektDtoer,
            førsteFraværsdag,
            KodeverkMapper.mapYtelsetype(ytelsetype),
            null,
            KodeverkMapper.mapForespørselStatus(ForespørselStatus.UNDER_BEHANDLING),
            førsteFraværsdag
        );
    }

    public InntektsmeldingEntitet hentInntektsmelding(long inntektsmeldingId) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
    }

    public List<InntektsmeldingResponseDto> hentInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));


        // for omsorgspenger ønsker vi å kun hente de inntektsmeldingene som er knyttet til forespørselen
        if (forespørsel.getYtelseType() == Ytelsetype.OMSORGSPENGER) {
            return forespørsel.getInntektsmeldinger().stream().map(im -> InntektsmeldingMapper.mapFraEntitet(im, forespørsel.getUuid())).toList();
        }

        var inntektsmeldinger = inntektsmeldingRepository.hentInntektsmeldinger(forespørsel.getAktørId(),
            forespørsel.getOrganisasjonsnummer(),
            forespørsel.getFørsteUttaksdato().orElseGet(forespørsel::getSkjæringstidspunkt),
            forespørsel.getYtelseType());
        return inntektsmeldinger.stream().map(im -> InntektsmeldingMapper.mapFraEntitet(im, forespørsel.getUuid())).toList();
    }

    public List<InntektsmeldingResponseDto> hentInntektsmeldingerForÅr(AktørIdEntitet aktørId,
                                                                       String arbeidsgiverIdent,
                                                                       int år,
                                                                       Ytelsetype ytelsetype) {

        var forespørsler = forespørselBehandlingTjeneste.finnForespørsler(aktørId, ytelsetype, arbeidsgiverIdent);

        // dersom det ikke finnes noen forespørsler er det ikke noe for GUI å vise
        if (forespørsler.isEmpty()) {
            return List.of();
        }

        // da denne koden er kun for GUI så er det ikke så viktig at forespørsel stemmer
        var førsteForespørsel = forespørsler.stream().findFirst();

        var inntektsmeldinger = inntektsmeldingRepository.hentInntektsmeldingerForÅr(aktørId,
            arbeidsgiverIdent,
            år,
            ytelsetype);

        return inntektsmeldinger.stream()
            .map(im -> InntektsmeldingMapper.mapFraEntitet(im, førsteForespørsel.get().getUuid()))
            .toList();
    }

        public byte[] hentPDF(long id) {
        var inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(id);
        return k9DokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet);
    }

    private InntektsmeldingDialogDto.InnsenderDto lagInnmelderDto(Ytelsetype ytelsetype) {
        if (!KontekstHolder.harKontekst() || !IdentType.EksternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new IllegalStateException("Mangler innlogget bruker kontekst.");
        }
        var pid = KontekstHolder.getKontekst().getUid();
        var personInfo = personTjeneste.hentPersonFraIdent(PersonIdent.fra(pid), ytelsetype);
        return new InntektsmeldingDialogDto.InnsenderDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.telefonnummer());
    }

    private InntektsmeldingDialogDto.InntektsopplysningerDto lagInntekterDto(UUID uuid,
                                                                             AktørIdEntitet aktørId,
                                                                             LocalDate skjæringstidspunkt,
                                                                             String organisasjonsnummer) {
        var inntektsopplysninger = inntektTjeneste.hentInntekt(aktørId, skjæringstidspunkt, LocalDate.now(),
            organisasjonsnummer);
        if (uuid == null) {
            LOG.info("Inntektsopplysninger for aktørId {} var {}", aktørId, inntektsopplysninger);
        } else {
            LOG.info("Inntektsopplysninger for forespørsel {} var {}", uuid, inntektsopplysninger);
        }
        var inntekter = inntektsopplysninger.måneder()
            .stream()
            .map(i -> new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(i.månedÅr().atDay(1),
                i.månedÅr().atEndOfMonth(),
                i.beløp(),
                i.status()))
            .toList();
        return new InntektsmeldingDialogDto.InntektsopplysningerDto(inntektsopplysninger.gjennomsnitt(), inntekter);
    }

    private InntektsmeldingDialogDto.OrganisasjonInfoResponseDto lagOrganisasjonDto(String organisasjonsnummer) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer);
        return new InntektsmeldingDialogDto.OrganisasjonInfoResponseDto(orgdata.navn(), orgdata.orgnr());
    }

    private InntektsmeldingDialogDto.PersonInfoResponseDto lagPersonDto(AktørIdEntitet aktørId, Ytelsetype ytelseType) {
        var personInfo = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelseType);
        return new InntektsmeldingDialogDto.PersonInfoResponseDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
    }

    public Optional<SlåOppArbeidstakerResponseDto> finnArbeidsforholdForFnr(PersonIdent fødselsnummer, Ytelsetype ytelsetype,
                                                                            LocalDate førsteFraværsdag) {
        // TODO Skal vi sjekke noe mtp kode 6/7
        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype);
        if (personInfo == null) {
            return Optional.empty();
        }
        var arbeidsforholdBrukerHarTilgangTil = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer, førsteFraværsdag);
        if (arbeidsforholdBrukerHarTilgangTil.isEmpty()) {
            return Optional.empty();
        }
        var arbeidsforholdDto = arbeidsforholdBrukerHarTilgangTil.stream()
            .map(a -> new SlåOppArbeidstakerResponseDto.ArbeidsforholdDto(organisasjonTjeneste.finnOrganisasjon(a.organisasjonsnummer()).navn(),
                a.organisasjonsnummer()))
            .collect(Collectors.toSet());
        return Optional.of(new SlåOppArbeidstakerResponseDto(personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            arbeidsforholdDto));
    }
}
