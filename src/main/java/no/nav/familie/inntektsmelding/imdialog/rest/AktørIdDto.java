package no.nav.familie.inntektsmelding.imdialog.rest;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

import java.util.Objects;

public final class AktørIdDto {
    private static final String VALID_REGEXP = "^\\d{13}$";
    @JsonValue
    private final @Pattern(regexp = VALID_REGEXP) String aktørId;

    public AktørIdDto(@Pattern(regexp = VALID_REGEXP) String aktørId) {
        this.aktørId = aktørId;
    }

    @JsonValue
    public @Pattern(regexp = VALID_REGEXP) String aktørId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (AktørIdDto) obj;
        return Objects.equals(this.aktørId, that.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return "AktørIdDto[" + "aktørId=" + aktørId + ']';
    }

}
