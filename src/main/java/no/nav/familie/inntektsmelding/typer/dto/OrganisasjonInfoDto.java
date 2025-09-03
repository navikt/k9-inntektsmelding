package no.nav.familie.inntektsmelding.typer.dto;

import jakarta.validation.constraints.NotNull;

public record OrganisasjonInfoDto(@NotNull String organisasjonNavn,
                                  @NotNull String organisasjonNummer) {
}
