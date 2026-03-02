package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Comparator;
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
import no.nav.familie.inntektsmelding.imdialog.rest.HentArbeidsforholdResponse;
import no.nav.familie.inntektsmelding.imdialog.rest.HentOpplysningerResponse;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester.ArbeidsforholdTjeneste;
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
public class GrunnlagTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(GrunnlagTjeneste.class);
    private static final List<ForespørselType> ARBEIDSGIVER_INITIERTE_FORESPØRSLER = List.of(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;
    private ArbeidstakerTjeneste arbeidstakerTjeneste;
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    GrunnlagTjeneste() {
    }

    @Inject
    public GrunnlagTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                            PersonTjeneste personTjeneste,
                            OrganisasjonTjeneste organisasjonTjeneste,
                            InntektTjeneste inntektTjeneste,
                            ArbeidstakerTjeneste arbeidstakerTjeneste, ArbeidsforholdTjeneste arbeidsforholdTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
    }

    public HentOpplysningerResponse hentOpplysninger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));
        return hentOpplysningerFraForespørsel(forespørsel);
    }

    private HentOpplysningerResponse hentOpplysningerFraForespørsel(ForespørselEntitet forespørsel) {
        var personInfo = finnPerson(forespørsel.getAktørId());
        var organisasjonInfo = finnOrganisasjonInfo(forespørsel.getOrganisasjonsnummer());
        var innsender = finnInnsender();
        var inntektsopplysninger = finnInntektsopplysninger(forespørsel.getUuid(), forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(), forespørsel.getOrganisasjonsnummer());

        return new HentOpplysningerResponse(personInfo,
            organisasjonInfo,
            innsender,
            inntektsopplysninger,
            forespørsel.getSkjæringstidspunkt(),
            KodeverkMapper.mapYtelsetype(forespørsel.getYtelseType()),
            forespørsel.getUuid(),
            KodeverkMapper.mapForespørselStatus(forespørsel.getStatus()),
            forespørsel.getForespørselType(),
            forespørsel.getFørsteUttaksdato().orElseGet(forespørsel::getSkjæringstidspunkt), forespørsel.getEtterspurtePerioder());
    }

    // Hvis en bruker har byttet jobb mens de mottar en ytelse, kan det hende at k9-sak ikke har opprettet en forespørsel for den nye arbeidsgiveren.
    // Da må arbeidsgiver sende kunne sende innteksmelding uten at det finnes en forespørsel.
    public HentOpplysningerResponse hentOpplysninger(PersonIdent fødselsnummer,
                                                     Ytelsetype ytelsetype,
                                                     LocalDate førsteFraværsdag,
                                                     OrganisasjonsnummerDto organisasjonsnummer,
                                                     ForespørselType forespørselType) {
        if (!ARBEIDSGIVER_INITIERTE_FORESPØRSLER.contains(forespørselType)) {
            throw new IllegalArgumentException("Kun arbeidsgiverinitierte forespørsler kan bruke denne metoden, forespørselType var " + forespørselType);
        }

        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer);

        var eksisterendeForepørsler = forespørselBehandlingTjeneste.finnAlleForespørsler(personInfo.aktørId(), ytelsetype, organisasjonsnummer.orgnr());
        var relevantForespørsel = eksisterendeForepørsler.stream()
            .filter(forespørsel -> innenforIntervall(førsteFraværsdag, forespørsel.getSkjæringstidspunkt()))
            .filter(forespørsel -> forespørsel.getStatus() != ForespørselStatus.UTGÅTT)
            .max(Comparator.comparing(ForespørselEntitet::getEndretTidspunkt));

        // Hvis k9-sak har opprettet forespørsel eller arbeidsgiver allerede har sendt inn inntektsmelding på denne datoen, så bruker vi vanlig flyt
        if (relevantForespørsel.isPresent()) {
            return hentOpplysningerFraForespørsel(relevantForespørsel.get());
        }

        var organisasjonInfo = finnOrganisasjonInfo(organisasjonsnummer.orgnr());
        var innsender = finnInnsender();
        var inntektsopplysninger = finnInntektsopplysninger(null, personInfo.aktørId(), førsteFraværsdag, organisasjonsnummer.orgnr());

        return new HentOpplysningerResponse(lagPersonInfoDto(personInfo),
            organisasjonInfo,
            innsender,
            inntektsopplysninger,
            førsteFraværsdag,
            KodeverkMapper.mapYtelsetype(ytelsetype),
            null,
            KodeverkMapper.mapForespørselStatus(ForespørselStatus.UNDER_BEHANDLING),
            forespørselType,
            førsteFraværsdag,
            null);
    }

    private boolean innenforIntervall(LocalDate nyFørsteFraværsdag, LocalDate eksisterendeForespørselStp) {
        if (eksisterendeForespørselStp == null) {
            return false;
        }

        return nyFørsteFraværsdag.isAfter((eksisterendeForespørselStp.minusWeeks(4)))
            && nyFørsteFraværsdag.isBefore(eksisterendeForespørselStp.plusWeeks(4));
    }

    private InnsenderDto finnInnsender() {
        if (!KontekstHolder.harKontekst() || !IdentType.EksternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new IllegalStateException("Mangler innlogget bruker kontekst.");
        }

        var pid = KontekstHolder.getKontekst().getUid();
        var personInfo = personTjeneste.hentPersonFraIdent(PersonIdent.fra(pid));

        return new InnsenderDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(), personInfo.telefonnummer());
    }

    private InntektsopplysningerDto finnInntektsopplysninger(UUID uuid,
                                                             AktørIdEntitet aktørId,
                                                             LocalDate skjæringstidspunkt,
                                                             String organisasjonsnummer) {
        var inntektsopplysninger = inntektTjeneste.hentInntekt(aktørId, skjæringstidspunkt, LocalDate.now(), organisasjonsnummer);

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
        return lagPersonInfoDto(personInfo);
    }

    private static PersonInfoDto lagPersonInfoDto(PersonInfo personInfo) {
        return new PersonInfoDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(), personInfo.fødselsnummer().getIdent(), personInfo.aktørId().getAktørId());
    }

    public Optional<HentArbeidsforholdResponse> finnArbeidsforholdForFnr(PersonIdent fødselsnummer, LocalDate førsteFraværsdag) {
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
            .map(a -> new HentArbeidsforholdResponse.ArbeidsforholdDto(organisasjonTjeneste.finnOrganisasjon(a.organisasjonsnummer()).navn(),
                a.organisasjonsnummer()))
            .collect(Collectors.toSet());

        return Optional.of(new HentArbeidsforholdResponse(personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            personInfo.kjønn(),
            arbeidsforholdDto));
    }

    public Optional<HentArbeidsforholdResponse> hentSøkerinfoOgOrganisasjonerArbeidsgiverHarTilgangTil(PersonInfo personInfo) {
        var organisasjonerArbeidsgiverHarTilgangTil = arbeidstakerTjeneste.finnOrganisasjonerArbeidsgiverHarTilgangTil(personInfo.fødselsnummer());

        var organisasjoner = organisasjonerArbeidsgiverHarTilgangTil.stream()
            .map(orgnrDto -> new HentArbeidsforholdResponse.ArbeidsforholdDto(organisasjonTjeneste.finnOrganisasjon(orgnrDto.orgnr()).navn(),
                orgnrDto.orgnr()))
            .collect(Collectors.toSet());

        return Optional.of(new HentArbeidsforholdResponse(personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            personInfo.kjønn(),
            organisasjoner));
    }

    public boolean finnesOrgnummerIAaregPåPerson(PersonIdent personIdent,
                                                 String organisasjonsnummer,
                                                 LocalDate førsteUttaksdato) {
        return arbeidsforholdTjeneste.hentArbeidsforhold(personIdent, førsteUttaksdato).stream()
            .filter(arbeidsforhold -> arbeidsforhold.organisasjonsnummer().equals(organisasjonsnummer))
            .anyMatch(arbeidsforhold -> inkludererDato(førsteUttaksdato,
                arbeidsforhold.ansettelsesperiode().fom(),
                arbeidsforhold.ansettelsesperiode().tom()));
    }

    private boolean inkludererDato(LocalDate førsteUttaksdato, LocalDate fom, LocalDate tom) {
        var fomLikEllerEtter = førsteUttaksdato.isEqual(fom) || førsteUttaksdato.isAfter(fom);
        var tomLikEllerFør = førsteUttaksdato.isEqual(tom) || førsteUttaksdato.isBefore(tom);
        return fomLikEllerEtter && tomLikEllerFør;
    }
}
