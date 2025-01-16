package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.List;

public record SlåOppArbeidstakerResponseDto(String fornavn, String mellomnavn, String etternavn, List<ArbeidsforholdDto> arbeidsforhold) {
    public record ArbeidsforholdDto(String organisasjonsnavn, String organisasjonsnummer) {}
}
