package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.ArbeidsforholdDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class ArbeidstakerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidstakerTjeneste.class);
    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;
    private AltinnTilgangTjeneste altinnTilgangTjeneste;

    public ArbeidstakerTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidstakerTjeneste(ArbeidsforholdTjeneste arbeidsforholdTjeneste, AltinnTilgangTjeneste altinnTilgangTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.altinnTilgangTjeneste = altinnTilgangTjeneste;
    }

    public List<ArbeidsforholdDto> finnArbeidsforholdForFnrMedTilgang(PersonIdent ident) {
        var alleArbeidsforhold = arbeidsforholdTjeneste.hentNåværendeArbeidsforhold(ident);
        LOG.info("Fant {} arbeidsforhold i Aa-registeret for {}", alleArbeidsforhold.size(), ident);

        if (alleArbeidsforhold.isEmpty()) {
            LOG.warn("Fant ingen arbeidsforhold i Aa-registeret for {}", ident);
            return Collections.emptyList();
        }

        var arbeidsforholdBrukerHarTilgangTil = alleArbeidsforhold
            .stream()
            .filter(dto -> altinnTilgangTjeneste.harTilgangTilBedriften(dto.organisasjonsnummer()))
            .toList();

        if (alleArbeidsforhold.size() > arbeidsforholdBrukerHarTilgangTil.size()) {
            LOG.info("Bruker har tilgang til {} av {} arbeidsforhold for {}", arbeidsforholdBrukerHarTilgangTil.size(), alleArbeidsforhold.size(), ident);
        }

        LOG.info("Returnerer informasjon om arbeidstaker og arbeidsforhold for {}", ident);
        return arbeidsforholdBrukerHarTilgangTil;
    }
}
