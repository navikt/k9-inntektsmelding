package no.nav.familie.inntektsmelding.typer;

import java.util.Objects;

public final class FagsakSaksnummer {

    private String saksnr;

    public FagsakSaksnummer() {
    }

    public FagsakSaksnummer(String saksnr) {
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
