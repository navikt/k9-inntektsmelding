package no.nav.familie.inntektsmelding.typer.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EndringsårsakerDto(@NotNull @Valid EndringsårsakDto årsak,
                                 LocalDate fom,
                                 LocalDate tom,
                                 LocalDate bleKjentFom) {
}
