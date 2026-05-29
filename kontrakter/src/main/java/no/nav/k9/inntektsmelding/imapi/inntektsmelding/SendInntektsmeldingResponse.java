package no.nav.k9.inntektsmelding.imapi.inntektsmelding;

import no.nav.k9.inntektsmelding.felles.FeilkodeDto;

import java.util.UUID;

public record SendInntektsmeldingResponse(boolean success, UUID inntektsmeldingUuid, FeilInfo feilinformasjon) {
    public record FeilInfo(FeilkodeDto feilkode, String feilmelding, String referanseId) {}
}
