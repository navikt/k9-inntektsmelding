package no.nav.familie.inntektsmelding.typer;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ArbeidsgiverDto (@JsonValue @NotNull @Digits(integer = 13, fraction = 0) @Pattern(regexp = REGEXP) String ident) {
    private static final String REGEXP = "^[0-9]*$";

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + maskerId() + ">";
    }

    private String maskerId() {
        if (ident == null) {
            return "";
        }
        var length = ident.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + ident.substring(length - 4);
    }

    public boolean erVirksomhet() {
        return ident.length() == 9;
    }

}
