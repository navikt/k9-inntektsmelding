package no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.HentInntektsopplysningerResponseDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.InnloggetBrukerDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.SlåOppArbeidstakerResponseDto;

@ApplicationScoped
public class RefusjonOmsorgsdagerService {
    private ArbeidstakerTjeneste arbeidstakerTjeneste;
    private PersonTjeneste personTjeneste;
    private InntektTjeneste inntektTjeneste;
    private InnloggetBrukerTjeneste innloggetBrukerTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private final static Logger LOG = LoggerFactory.getLogger(RefusjonOmsorgsdagerService.class);

    @Inject
    public RefusjonOmsorgsdagerService(ArbeidstakerTjeneste arbeidstakerTjeneste, PersonTjeneste personTjeneste, InntektTjeneste inntektTjeneste, InnloggetBrukerTjeneste innloggetBrukerTjeneste, OrganisasjonTjeneste organisasjonTjeneste) {
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
        this.personTjeneste = personTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.innloggetBrukerTjeneste = innloggetBrukerTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public RefusjonOmsorgsdagerService() {
        // CDI
    }

    public SlåOppArbeidstakerResponseDto hentArbeidstaker(PersonIdent fødselsnummer) {
        LOG.info("Slår opp arbeidstaker");

        var arbeidsforhold = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer, LocalDate.now());
        var unikeArbeidsforhold = filtrerUnikeArbeidsforhold(arbeidsforhold);
        var arbeidsforholdMedOrgnavn = unikeArbeidsforhold.stream()
            .map(arbeidsforholdDto -> new SlåOppArbeidstakerResponseDto.ArbeidsforholdDto(
                arbeidsforholdDto.organisasjonsnummer(),
                organisasjonTjeneste.finnOrganisasjon(arbeidsforholdDto.organisasjonsnummer()).navn()
            ))
            .toList();

        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer);
        if (arbeidsforholdMedOrgnavn.isEmpty() || personInfo == null) {
            return null;
        }

        return new SlåOppArbeidstakerResponseDto(
            new SlåOppArbeidstakerResponseDto.Personinformasjon(
                personInfo.fornavn(),
                personInfo.mellomnavn(),
                personInfo.etternavn(),
                personInfo.fødselsnummer().getIdent(),
                personInfo.aktørId().getAktørId()
            ),
            arbeidsforholdMedOrgnavn
        );
    }

    private List<ArbeidsforholdDto> filtrerUnikeArbeidsforhold(List<ArbeidsforholdDto> arbeidsforhold) {
        Set<String> unikeOrgnr = new HashSet<>();
        return arbeidsforhold.stream()
            .filter(arbeidsforholdDto -> unikeOrgnr.add(arbeidsforholdDto.organisasjonsnummer()))
            .collect(Collectors.toList());
    }

    public InnloggetBrukerDto hentInnloggetBruker(String organisasjonsnummer) {
        return innloggetBrukerTjeneste.hentInnloggetBruker(Ytelsetype.OMSORGSPENGER, organisasjonsnummer);
    }

    public HentInntektsopplysningerResponseDto hentInntektsopplysninger(PersonIdent fødselsnummer, String organisasjonsnummer, LocalDate skjæringstidspunkt) {
        var person = personTjeneste.hentPersonFraIdent(fødselsnummer);
        var arbeidsforhold = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(
            fødselsnummer,
            skjæringstidspunkt
        );
        if (arbeidsforhold.isEmpty() || person == null) {
            return null;
        }
        var inntektRespons = inntektTjeneste.hentInntekt(
            person.aktørId(),
            skjæringstidspunkt,
            LocalDate.now(),
            organisasjonsnummer
        );
        var inntekter  = inntektRespons.måneder()
            .stream()
            .map(i -> new HentInntektsopplysningerResponseDto.MånedsinntektDto(i.månedÅr().atDay(1),
                i.månedÅr().atEndOfMonth(),
                i.beløp(),
                i.status()))
            .toList();

        return new HentInntektsopplysningerResponseDto(inntektRespons.gjennomsnitt(), inntekter);
    }
}
