package no.nav.familie.inntektsmelding.pip;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.ArbeidsgiverAltinnTilgangerKlient;

@Dependent
public class AltinnTilgangTjeneste {

    private final ArbeidsgiverAltinnTilgangerKlient altinnKlient;

    public AltinnTilgangTjeneste() {
        this(ArbeidsgiverAltinnTilgangerKlient.instance());
    }

    public AltinnTilgangTjeneste(ArbeidsgiverAltinnTilgangerKlient altinnKlient) {
        this.altinnKlient = altinnKlient;
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        return altinnKlient.harTilgangTilBedriften(orgNr);
    }

    public boolean manglerTilgangTilBedriften(String orgNr) {
        return !harTilgangTilBedriften(orgNr);
    }
}
