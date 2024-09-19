package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.util.UUID;

public interface Tilgang {
    /**
     * Her hentes det en organisasjonsnummer fra forespørsel (PIP tjeneste) og så sjekker man mot altinn om brukeren har tilgang til denne bedriften.
     *
     * @param forespørselUuid - IM forespørsel som ble generert når fagsystemet bestilte IM.
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen er ikke gitt.
     */
    void sjekkOmArbeidsgiverHarTilgangTilBedrift(UUID forespørselUuid);

}
