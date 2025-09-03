package no.nav.familie.inntektsmelding.typer.dto;

import jakarta.validation.constraints.NotNull;

public record InnsenderDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn, String telefon) {
}
