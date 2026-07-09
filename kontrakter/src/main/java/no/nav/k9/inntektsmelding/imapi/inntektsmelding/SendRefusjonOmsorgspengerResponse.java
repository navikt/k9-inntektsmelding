package no.nav.k9.inntektsmelding.imapi.inntektsmelding;

import java.util.UUID;

import no.nav.k9.inntektsmelding.felles.FeilInfo;

public record SendRefusjonOmsorgspengerResponse(boolean success,
                                                UUID inntektsmeldingUuid,
                                                FeilInfo feilinformasjon) {
}
