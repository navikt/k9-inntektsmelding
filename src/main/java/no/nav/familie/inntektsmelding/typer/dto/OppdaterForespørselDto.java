package no.nav.familie.inntektsmelding.typer.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OppdaterForespørselDto(@NotNull LocalDate skjæringstidspunkt,
                                     @NotNull @Valid OrganisasjonsnummerDto orgnr,
                                     @NotNull ForespørselAksjon aksjon,
                                     @Valid List<PeriodeDto> etterspurtePerioder) {

    public OppdaterForespørselDto(@NotNull LocalDate skjæringstidspunkt,
                                  @NotNull @Valid OrganisasjonsnummerDto orgnr,
                                  @NotNull ForespørselAksjon aksjon) {
        this(skjæringstidspunkt, orgnr, aksjon, null);
    }
}
