package no.nav.familie.inntektsmelding.imdialog.rest;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

public record AktørIdDto(
    @JsonValue @NotNull @Pattern(regexp = VALID_REGEXP, message = "aktørId ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String aktørId) {
    private static final String VALID_REGEXP = "^\\d{13}$";
}
