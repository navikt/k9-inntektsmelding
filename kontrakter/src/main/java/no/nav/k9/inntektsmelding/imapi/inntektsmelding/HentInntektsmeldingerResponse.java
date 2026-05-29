package no.nav.k9.inntektsmelding.imapi.inntektsmelding;

import java.util.List;

public record HentInntektsmeldingerResponse(List<InntektsmeldingDto> inntektsmeldinger) {
}

