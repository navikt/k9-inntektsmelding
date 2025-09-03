package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

public record InnloggetBrukerDto(
    String fornavn,
    String mellomnavn,
    String etternavn,
    String telefon,
    String organisasjonsnummer,
    String organisasjonsnavn) {
}
