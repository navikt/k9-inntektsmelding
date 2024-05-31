package no.nav.familie.inntektsmelding.typer;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

public record OrganisasjonsnummerDto(@JsonValue @NotNull @Pattern(regexp = VALID_REGEXP, message = "orgnr ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String orgnr) {
    private static final String VALID_REGEXP = "^\\d{9}$";

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (OrganisasjonsnummerDto) obj;
        return Objects.equals(this.orgnr, that.orgnr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgnr);
    }

    @Override
    public String toString() {
        return "Organisasjonsnummer[" + "orgnr=" + orgnr.substring(0, Math.min(orgnr.length(), 3)) + "...]";
    }


}
