package no.nav.familie.inntektsmelding.pip;

import jakarta.enterprise.context.Dependent;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnAutoriseringKlient;

@Dependent
public class TilgangTjeneste {

    private AltinnAutoriseringKlient altinnKlient;

    public TilgangTjeneste() {
        this.altinnKlient = AltinnAutoriseringKlient.instance();
    }

    public boolean harTilgangTilBedriften(String orgNr) {
        return altinnKlient.harTilgangTilBedriften(orgNr);
    }
}
