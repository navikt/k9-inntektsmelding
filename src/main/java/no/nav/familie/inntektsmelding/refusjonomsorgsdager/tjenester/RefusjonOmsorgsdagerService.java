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
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.HentInnloggetBrukerResponse;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.HentInntektsopplysningerResponse;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.SlåOppArbeidstakerResponse;

@ApplicationScoped
public class RefusjonOmsorgsdagerService {
    private ArbeidstakerTjeneste arbeidstakerTjeneste;
    private PersonTjeneste personTjeneste;
    private InntektTjeneste inntektTjeneste;
    private InnloggetBrukerTjeneste innloggetBrukerTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private final static Logger LOG = LoggerFactory.getLogger(RefusjonOmsorgsdagerService.class);

    @Inject
    public RefusjonOmsorgsdagerService(ArbeidstakerTjeneste arbeidstakerTjeneste,
                                       PersonTjeneste personTjeneste,
                                       InntektTjeneste inntektTjeneste,
                                       InnloggetBrukerTjeneste innloggetBrukerTjeneste,
                                       OrganisasjonTjeneste organisasjonTjeneste) {
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
        this.personTjeneste = personTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.innloggetBrukerTjeneste = innloggetBrukerTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public RefusjonOmsorgsdagerService() {
        // CDI
    }

    public SlåOppArbeidstakerResponse hentArbeidstaker(PersonIdent fødselsnummer) {
        LOG.info("Slår opp arbeidstaker");

        var alleArbeidsforhold = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer, LocalDate.now());
        var unikeArbeidsforhold = filtrerUnikeArbeidsforhold(alleArbeidsforhold);
        var arbeidsforholdMedOrgnavn = unikeArbeidsforhold.stream()
            .map(arbeidsforhold -> new SlåOppArbeidstakerResponse.ArbeidsforholdDto(
                arbeidsforhold.organisasjonsnummer(),
                organisasjonTjeneste.finnOrganisasjon(arbeidsforhold.organisasjonsnummer()).navn()
            ))
            .toList();

        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer);
        if (arbeidsforholdMedOrgnavn.isEmpty() || personInfo == null) {
            return null;
        }

        return new SlåOppArbeidstakerResponse(
            new SlåOppArbeidstakerResponse.Personinformasjon(
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

    public HentInnloggetBrukerResponse hentInnloggetBruker(String organisasjonsnummer) {
        var innloggetBruker = innloggetBrukerTjeneste.hentInnloggetBruker(Ytelsetype.OMSORGSPENGER, organisasjonsnummer);

        return new HentInnloggetBrukerResponse(
            innloggetBruker.fornavn(),
            innloggetBruker.mellomnavn(),
            innloggetBruker.etternavn(),
            innloggetBruker.telefon(),
            innloggetBruker.organisasjonsnummer(),
            innloggetBruker.organisasjonsnavn()
        );
    }

    public HentInntektsopplysningerResponse hentInntektsopplysninger(PersonIdent fødselsnummer,
                                                                     String organisasjonsnummer,
                                                                     LocalDate skjæringstidspunkt) {
        var person = personTjeneste.hentPersonFraIdent(fødselsnummer);
        var arbeidsforhold = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(
            fødselsnummer,
            skjæringstidspunkt
        );
        if (arbeidsforhold.isEmpty() || person == null) {
            return null;
        }
        var inntekt = inntektTjeneste.hentInntekt(
            person.aktørId(),
            skjæringstidspunkt,
            LocalDate.now(),
            organisasjonsnummer
        );
        var inntekterPerMåned = inntekt.måneder()
            .stream()
            .map(i -> new HentInntektsopplysningerResponse.MånedsinntektDto(i.månedÅr().atDay(1),
                i.månedÅr().atEndOfMonth(),
                i.beløp(),
                i.status()))
            .toList();

        return new HentInntektsopplysningerResponse(inntekt.gjennomsnitt(), inntekterPerMåned);
    }
}
