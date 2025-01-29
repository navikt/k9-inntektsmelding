package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import jakarta.validation.constraints.NotNull;

public record InnloggetBrukerDto(
    @NotNull String fornavn,
    String mellomnavn,
    @NotNull String etternavn,
    String telefon,
    @NotNull String organisasjonsnummer,
    @NotNull String organisasjonsnavn) {
}
