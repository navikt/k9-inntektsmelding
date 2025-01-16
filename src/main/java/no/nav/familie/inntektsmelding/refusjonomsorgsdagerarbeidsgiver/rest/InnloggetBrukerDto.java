package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest;

import jakarta.validation.constraints.NotNull;

public record InnloggetBrukerDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn, String telefon, @NotNull String organisasjonsnummer, @NotNull String organisasjonsnavn) {
    public static InnloggetBrukerDto tom() {
        return new InnloggetBrukerDto(null, null, null, null, null, null);
    }
}
