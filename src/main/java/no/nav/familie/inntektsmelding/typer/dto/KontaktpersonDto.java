package no.nav.familie.inntektsmelding.typer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KontaktpersonDto(@NotNull @Size(max = 100) String navn,
                               @NotNull @Size(max = 100) String telefonnummer) {
}
