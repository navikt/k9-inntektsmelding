package no.nav.familie.inntektsmelding.typer.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record PeriodeDto(@NotNull LocalDate fom,
                         @NotNull LocalDate tom) {

    @AssertTrue(message = "fom dato må være før eller lik tom dato")
    private boolean isValid() {
        return fom.isBefore(tom) || fom.isEqual(tom);
    }

    public boolean inneholderDato(LocalDate dato) {
        return (dato.isEqual(fom) || dato.isAfter(fom)) && (dato.isEqual(tom) || dato.isBefore(tom));
    }
}
