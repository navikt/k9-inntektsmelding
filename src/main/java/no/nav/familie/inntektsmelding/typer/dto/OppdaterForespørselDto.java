package no.nav.familie.inntektsmelding.typer.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OppdaterForespørselDto(@NotNull LocalDate skjæringstidspunkt,
                                     @NotNull @Valid OrganisasjonsnummerDto orgnr,
                                     @NotNull ForespørselAksjon aksjon,
                                     @Valid OmsorgspengerDataDto omsorgspengerData) {

    public OppdaterForespørselDto(@NotNull LocalDate skjæringstidspunkt,
                                  @NotNull @Valid OrganisasjonsnummerDto orgnr,
                                  @NotNull ForespørselAksjon aksjon) {
        this(skjæringstidspunkt, orgnr, aksjon, null);
    }
}
