package no.nav.familie.inntektsmelding.refusjonomsorgsdager.tjenester;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.InnloggetBrukerDto;
import no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest.SlåOppArbeidstakerResponseDto;

@ApplicationScoped
public class RefusjonOmsorgsdagerService {
    private ArbeidstakerTjeneste arbeidstakerTjeneste;
    private PersonTjeneste personTjeneste;
    private InntektTjeneste inntektTjeneste;
    private InnloggetBrukerTjeneste innloggetBrukerTjeneste;
    private final static Logger LOG = LoggerFactory.getLogger(RefusjonOmsorgsdagerService.class);

    @Inject
    public RefusjonOmsorgsdagerService(ArbeidstakerTjeneste arbeidstakerTjeneste, PersonTjeneste personTjeneste, InntektTjeneste inntektTjeneste, InnloggetBrukerTjeneste innloggetBrukerTjeneste) {
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
        this.personTjeneste = personTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.innloggetBrukerTjeneste = innloggetBrukerTjeneste;
    }

    public RefusjonOmsorgsdagerService() {
        // CDI
    }

    public SlåOppArbeidstakerResponseDto hentArbeidstaker(PersonIdent fødselsnummer) {
        LOG.info("Slår opp arbeidstaker med fødselsnummer {}", fødselsnummer);

        var arbeidsforhold = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer,
            LocalDate.now());
        var personInfo = personTjeneste.hentPersonFraIdent(fødselsnummer, Ytelsetype.OMSORGSPENGER);
        if (arbeidsforhold.isEmpty() || personInfo == null) {
            return null;
        }

        return new SlåOppArbeidstakerResponseDto(
            new SlåOppArbeidstakerResponseDto.Personinformasjon(
                personInfo.fornavn(),
                personInfo.mellomnavn(),
                personInfo.etternavn(),
                personInfo.fødselsnummer().getIdent()
            ),
            arbeidsforhold
        );
    }

    public InnloggetBrukerDto hentInnloggetBruker(String organisasjonsnummer) {
        return innloggetBrukerTjeneste.hentInnloggetBruker(Ytelsetype.OMSORGSPENGER, organisasjonsnummer);
    }

    public Inntektsopplysninger hentInntektsopplysninger(PersonIdent fødselsnummer, String organisasjonsnummer, LocalDate skjæringstidspunkt) {
        var person = personTjeneste.hentPersonFraIdent(fødselsnummer, Ytelsetype.OMSORGSPENGER);
        var arbeidsforhold = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(
            fødselsnummer,
            skjæringstidspunkt
        );
        if (arbeidsforhold.isEmpty() || person == null) {
            return null;
        }
        return inntektTjeneste.hentInntekt(
            person.aktørId(),
            skjæringstidspunkt,
            LocalDate.now(),
            organisasjonsnummer
        );
    }
}
