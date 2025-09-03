package no.nav.familie.inntektsmelding.typer.dto;

import jakarta.validation.constraints.NotNull;

public record PersonInfoDto(@NotNull String fornavn,
                            @NotNull String mellomnavn,
                            @NotNull String etternavn,
                            @NotNull String fødselsnummer,
                            @NotNull String aktørId) {
}
