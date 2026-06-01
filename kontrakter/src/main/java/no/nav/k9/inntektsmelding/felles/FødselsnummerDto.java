package no.nav.k9.inntektsmelding.felles;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;

public record FødselsnummerDto(@JsonValue @NotNull @Pattern(regexp = "^\\d{11}$") @NotNull String fnr) {
    @Override
    public String toString() {
        return "";
    }
}
