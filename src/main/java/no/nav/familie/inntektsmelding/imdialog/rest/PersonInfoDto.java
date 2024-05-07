package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.constraints.NotNull;

public record PersonInfoDto(@NotNull String navn, @NotNull String fødselsnummer, @NotNull String aktørId) {}
