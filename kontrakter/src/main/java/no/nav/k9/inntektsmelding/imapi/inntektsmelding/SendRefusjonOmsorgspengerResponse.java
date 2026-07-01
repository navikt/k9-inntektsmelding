package no.nav.k9.inntektsmelding.imapi.inntektsmelding;

import java.util.UUID;

import no.nav.k9.inntektsmelding.felles.FeilkodeDto;

public record SendRefusjonOmsorgspengerResponse(boolean success, UUID inntektsmeldingUuid, FeilInfo feilinformasjon) {
    public record FeilInfo(FeilkodeDto feilkode, String feilmelding, String referanseId) {}
}
