package no.nav.familie.inntektsmelding.server.authz;

import no.nav.familie.inntektsmelding.server.authz.api.TilgangsstyringInputType;

public enum TilgangsstyringInputTyper implements TilgangsstyringInputType {
    FORESPORSEL_ID("forespørselId");

    private final boolean maskerOutput;
    private final String sporingsloggKode;

    TilgangsstyringInputTyper(String sporingsloggKode) {
        this.sporingsloggKode = sporingsloggKode;
        this.maskerOutput = false;
    }

    TilgangsstyringInputTyper(String sporingsloggKode, boolean maskerOutput) {
        this.sporingsloggKode = sporingsloggKode;
        this.maskerOutput = maskerOutput;
    }

    @Override
    public boolean erMaskert() {
        return false;
    }
}
