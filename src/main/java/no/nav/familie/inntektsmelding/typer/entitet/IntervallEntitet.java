package no.nav.familie.inntektsmelding.typer.entitet;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import no.nav.vedtak.konfig.Tid;

@Embeddable
public class IntervallEntitet {

    @Column(name = "fom")
    private LocalDate fomDato;

    @Column(name = "tom")
    private LocalDate tomDato;


    public IntervallEntitet() {
        //hibernate
    }

    public LocalDate getFom() {
        return fomDato;
    }

    public LocalDate getTom() {
        return tomDato;
    }

    private IntervallEntitet(LocalDate fomDato, LocalDate tomDato) {
        if (fomDato == null) {
            throw new IllegalArgumentException("Fra og med dato må være satt.");
        } else if (tomDato == null) {
            throw new IllegalArgumentException("Til og med dato må være satt.");
        } else if (tomDato.isBefore(fomDato)) {
            throw new IllegalArgumentException("Til og med dato før fra og med dato.");
        } else {
            this.fomDato = fomDato;
            this.tomDato = tomDato;
        }
    }

    public static IntervallEntitet fraOgMedTilOgMed(LocalDate fomDato, LocalDate tomDato) {
        return new IntervallEntitet(fomDato, tomDato);
    }

    public static IntervallEntitet fraOgMed(LocalDate fomDato) {
        return new IntervallEntitet(fomDato, Tid.TIDENES_ENDE);
    }

    protected IntervallEntitet lagNyPeriode(LocalDate fomDato, LocalDate tomDato) {
        return fraOgMedTilOgMed(fomDato, tomDato);
    }
}
