package no.nav.familie.inntektsmelding.imdialog.rest;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OrganisasjonsnummerDto(
    @JsonValue @NotNull @Pattern(regexp = "\\d{9}$", message = "organisasjonsnummer ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String organisasjonsnummer) {
}
