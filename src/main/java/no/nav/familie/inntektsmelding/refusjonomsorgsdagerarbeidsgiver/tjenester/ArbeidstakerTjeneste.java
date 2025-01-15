package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.SlåOppArbeidstakerResponseDto;

@ApplicationScoped
public class ArbeidstakerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidstakerTjeneste.class);
    private PersonTjeneste personTjeneste;
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    public ArbeidstakerTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidstakerTjeneste(PersonTjeneste personTjeneste, ArbeidsforholdTjeneste arbeidsforholdTjeneste, AltinnTilgangTjeneste altinnTilgangTjeneste) {
        this.personTjeneste = personTjeneste;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.altinnTilgangTjeneste = altinnTilgangTjeneste;
    }

    public SlåOppArbeidstakerResponseDto slåOppArbeidstaker(PersonIdent ident, Ytelsetype ytelseType) {
        var personInfo = personTjeneste.hentPersonFraIdent(ident, ytelseType);

        if (personInfo == null) {
            LOG.warn("Fant ikke personinformasjon for {}", ident);
            return null;
        }

        var alleArbeidsforhold = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(ident);
        LOG.info("Fant {} arbeidsforhold i Aa-registeret for {}", alleArbeidsforhold.size(), ident);

        if (alleArbeidsforhold.isEmpty()) {
            LOG.warn("Fant ingen arbeidsforhold i Aa-registeret for {}", ident);
            return null;
        }

        var arbeidsforholdBrukerHarTilgangTil = alleArbeidsforhold
            .stream()
            .filter(dto -> altinnTilgangTjeneste.harTilgangTilBedriften(dto.underenhetId()))
            .toList();

        if (alleArbeidsforhold.size() > arbeidsforholdBrukerHarTilgangTil.size()) {
            LOG.info("Bruker har tilgang til {} av {} arbeidsforhold for {}", arbeidsforholdBrukerHarTilgangTil.size(), alleArbeidsforhold.size(), ident);
            if (arbeidsforholdBrukerHarTilgangTil.isEmpty()) {
                return null;
            }
        }

        LOG.info("Returnerer informasjon om arbeidstaker og arbeidsforhold for {}", personInfo.fødselsnummer());
        return new SlåOppArbeidstakerResponseDto(
            personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            arbeidsforholdBrukerHarTilgangTil
        );
    }
}
