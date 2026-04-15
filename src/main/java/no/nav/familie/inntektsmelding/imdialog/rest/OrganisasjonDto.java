package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.constraints.NotNull;

public record OrganisasjonDto(@NotNull String organisasjonsnavn, @NotNull String organisasjonsnummer) {
}

