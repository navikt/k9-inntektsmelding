package no.nav.familie.inntektsmelding.typer;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class FagsakSaksnummer {

    private static final String REGEXP = "^[\\p{Alnum}]+$";

    @NotNull
    @Pattern(regexp = REGEXP, message = "Saksnummer [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String saksnr;

    public FagsakSaksnummer() {
    }

    public FagsakSaksnummer(String saksnr) {
        Objects.requireNonNull(saksnr);
        this.saksnr = saksnr;
    }

    public String getSaksnr() {
        return saksnr;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (FagsakSaksnummer) obj;
        return Objects.equals(this.saksnr, that.saksnr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnr);
    }

    @Override
    public String toString() {
        return "FagsakSaksnummer[" + "saksnr=" + saksnr + ']';
    }
}
