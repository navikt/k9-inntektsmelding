package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import java.util.List;

public record SlåOppArbeidstakerResponseDto(Personinformasjon personinformasjon, List<ArbeidsforholdDto> arbeidsforhold) {
    public record Personinformasjon(
        String fornavn,
        String mellomnavn,
        String etternavn) {
    }
}
