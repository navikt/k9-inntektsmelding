package no.nav.familie.inntektsmelding.typer.dto;

import jakarta.validation.constraints.NotNull;

public record AnonymPersonInfoDto(@NotNull String fødselsnummer,
                                  @NotNull String aktørId) {
}
