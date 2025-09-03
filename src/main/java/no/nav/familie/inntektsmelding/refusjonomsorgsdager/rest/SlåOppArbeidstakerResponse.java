package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import java.util.List;

public record SlåOppArbeidstakerResponse(Personinformasjon personinformasjon, List<ArbeidsforholdDto> arbeidsforhold) {
    public record Personinformasjon(
        String fornavn,
        String mellomnavn,
        String etternavn,
        String fødselsnummer,
        String aktørId) {
    }
    public record ArbeidsforholdDto(String organisasjonsnummer, String organisasjonsnavn) {
    }
}
