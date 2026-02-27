package no.nav.familie.inntektsmelding.pip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnAutoriseringKlient;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;

@Dependent
public class AltinnTilgangTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AltinnTilgangTjeneste.class);

    private final AltinnAutoriseringKlient altinnKlient;
    private final ArbeidsgiverAltinnTilgangerKlient nyAltinnKlient;

    public AltinnTilgangTjeneste() {
        this(AltinnAutoriseringKlient.instance(), ArbeidsgiverAltinnTilgangerKlient.instance());
    }

    public AltinnTilgangTjeneste(AltinnAutoriseringKlient altinnKlient, ArbeidsgiverAltinnTilgangerKlient nyAltinnKlient) {
        this.altinnKlient = altinnKlient;
        this.nyAltinnKlient = nyAltinnKlient;
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        boolean gammelHarTilgang = altinnKlient.harTilgangTilBedriften(orgNr);
        try {
            boolean nyHarTilgang = nyAltinnKlient.harTilgangTilBedriften(orgNr);
            if (nyHarTilgang != gammelHarTilgang) {
                LOG.warn("ALTINN: gammelHarTilgang: {}, nyHarTilgang: {}", gammelHarTilgang, nyHarTilgang);
            }
            return nyHarTilgang;
        } catch (Exception e) {
            LOG.warn("Kall på ny altinnklient feilet", e);
        }
        return gammelHarTilgang;
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }
}
