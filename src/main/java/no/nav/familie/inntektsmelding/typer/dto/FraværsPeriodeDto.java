package no.nav.familie.inntektsmelding.typer.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record FraværsPeriodeDto(@NotNull LocalDate fom,
                                @NotNull LocalDate tom) {
    public boolean overlapper(FraværsPeriodeDto annenFraværsPeriode) {
        return (fom.isBefore(annenFraværsPeriode.tom) || fom.isEqual(annenFraværsPeriode.tom)) &&
            (tom.isAfter(annenFraværsPeriode.fom) || tom.isEqual(annenFraværsPeriode.fom));
    }

    public boolean inneholder(LocalDate dato) {
        return (fom.isBefore(dato) || fom.isEqual(dato)) && (tom.isAfter(dato) || tom.isEqual(dato));
    }

    @AssertTrue(message = "fom er før tom")
    private boolean isValidFomErFørTom() {
        return fom.isBefore(tom) || fom.isEqual(tom);
    }
}
