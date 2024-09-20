package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.util.UUID;

public interface Tilgang {
    /**
     * Her hentes organisasjonsnummeret fra forespørselen sendt inn av fagsystemet (fra PIP-tjenesten),
     * og deretter sjekkes det i Altinn om brukeren som initierte kallet, har tilgang til den aktuelle bedriften.
     *
     * @param forespørselUuid - IM forespørsel som ble generert når fagsystemet bestilte IM.
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtArbeidsgiverHarTilgangTilBedrift(UUID forespørselUuid);

    /**
     * Her hentes organisasjonsnummer knyttet til en tidligere innsendt inntektsmelding (fra PIP tjenesten),
     * og deretter sjekkes det i Altinn om brukeren som initierte kallet, har tilgang til den aktuelle bedriften.
     *
     * @param inntektsmeldingId - IM id som ble tidligere innsendt.
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtArbeidsgiverHarTilgangTilBedrift(long inntektsmeldingId);

    /**
     * Sjekker om saksbehandleren som prøver å utføre operasjonen har en DRIFT rolle.
     * Brukes kun i swagger sammenheng.
     *
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtSaksbehandlerHarRollenDrift();

}
