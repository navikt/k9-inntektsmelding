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

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SlåOppArbeidstakerResponseDto;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.K9DokgenTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.InnsenderDto;
import no.nav.familie.inntektsmelding.typer.dto.InntektsopplysningerDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.MånedsinntektDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonInfoDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PersonInfoDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
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
                                   ArbeidstakerTjeneste arbeidstakerTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.k9DokgenTjeneste = k9DokgenTjeneste;
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
    }

    public InntektsmeldingDialogDto lagDialogDto(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException(
                "Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));
        var personInfo = finnPerson(forespørsel.getAktørId());
        var organisasjonInfo = finnOrganisasjonInfo(forespørsel.getOrganisasjonsnummer());
        var innsender = finnInnsender();
        var inntektsopplysninger = finnInntektsopplysninger(forespørsel.getUuid(),
            forespørsel.getAktørId(),
            forespørsel.getSkjæringstidspunkt(),
            forespørsel.getOrganisasjonsnummer());

        return new InntektsmeldingDialogDto(personInfo,
            organisasjonInfo,
            innsender,
            inntektsopplysninger,
            forespørsel.getSkjæringstidspunkt(),
            KodeverkMapper.mapYtelsetype(forespørsel.getYtelseType()),
            forespørsel.getUuid(),
            KodeverkMapper.mapForespørselStatus(forespørsel.getStatus()),
            forespørsel.getFørsteUttaksdato().orElseGet(forespørsel::getSkjæringstidspunkt),
            forespørsel.getEtterspurtePerioder());
    }

    public InntektsmeldingDialogDto lagArbeidsgiverinitiertDialogDto(PersonIdent fødselsnummer,
                                                                     Ytelsetype ytelsetype,
                                                                     LocalDate førsteFraværsdag,
                                                                     OrganisasjonsnummerDto organisasjonsnummer) {
        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer);

        var eksisterendeForepørsler = forespørselBehandlingTjeneste.finnForespørslerUnderBehandling(personInfo.aktørId(),
            ytelsetype,
            organisasjonsnummer.orgnr());
        var forespørslerSomMatcherFraværsdag = eksisterendeForepørsler.stream()
            .filter(f -> førsteFraværsdag.equals(f.getFørsteUttaksdato()
                .orElse(f.getSkjæringstidspunkt()))) // TODO: sjekk for et større intervall etterhvert
            .toList();

        if (!forespørslerSomMatcherFraværsdag.isEmpty()) {
            var forespørsel = forespørslerSomMatcherFraværsdag.getFirst();
            return lagDialogDto(forespørsel.getUuid());
        }

        var personDto = new PersonInfoDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(), personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
        var organisasjonInfo = finnOrganisasjonInfo(organisasjonsnummer.orgnr());
        var innsender = finnInnsender();
        var inntektsopplysninger = finnInntektsopplysninger(null, personInfo.aktørId(), førsteFraværsdag, organisasjonsnummer.orgnr());
        return new InntektsmeldingDialogDto(personDto,
            organisasjonInfo,
            innsender,
            inntektsopplysninger,
            førsteFraværsdag,
            KodeverkMapper.mapYtelsetype(ytelsetype),
            null,
            KodeverkMapper.mapForespørselStatus(ForespørselStatus.UNDER_BEHANDLING),
            førsteFraværsdag,
            null
        );
    }

    public InntektsmeldingEntitet hentInntektsmelding(long inntektsmeldingId) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
    }

    public List<InntektsmeldingResponseDto> hentInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));

        var inntektsmeldinger = forespørsel.getInntektsmeldinger();

        return inntektsmeldinger
            .stream()
            .map(im -> InntektsmeldingMapper.mapFraEntitet(im, forespørsel.getUuid()))
            .toList();
    }

    public List<InntektsmeldingResponseDto> hentInntektsmeldingerForÅr(AktørIdEntitet aktørId,
                                                                       String arbeidsgiverIdent,
                                                                       int år,
                                                                       Ytelsetype ytelsetype) {

        var forespørsler = forespørselBehandlingTjeneste.finnAlleForespørsler(aktørId, ytelsetype, arbeidsgiverIdent);

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

    private InnsenderDto finnInnsender() {
        if (!KontekstHolder.harKontekst() || !IdentType.EksternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new IllegalStateException("Mangler innlogget bruker kontekst.");
        }
        var pid = KontekstHolder.getKontekst().getUid();
        var personInfo = personTjeneste.hentPersonFraIdent(PersonIdent.fra(pid));
        return new InnsenderDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.telefonnummer());
    }

    private InntektsopplysningerDto finnInntektsopplysninger(UUID uuid,
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
            .map(i -> new MånedsinntektDto(i.månedÅr().atDay(1),
                i.månedÅr().atEndOfMonth(),
                i.beløp(),
                i.status()))
            .toList();
        return new InntektsopplysningerDto(inntektsopplysninger.gjennomsnitt(), inntekter);
    }

    private OrganisasjonInfoDto finnOrganisasjonInfo(String organisasjonsnummer) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer);
        return new OrganisasjonInfoDto(orgdata.navn(), orgdata.orgnr());
    }

    private PersonInfoDto finnPerson(AktørIdEntitet aktørId) {
        var personInfo = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        return new PersonInfoDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
    }

    public Optional<SlåOppArbeidstakerResponseDto> finnArbeidsforholdForFnr(PersonIdent fødselsnummer, Ytelsetype ytelsetype,
                                                                            LocalDate førsteFraværsdag) {
        // TODO Skal vi sjekke noe mtp kode 6/7
        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer);
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
