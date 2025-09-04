package no.nav.familie.inntektsmelding.typer.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record InntektsopplysningerDto(@Valid BigDecimal gjennomsnittLønn, @NotNull @Valid List<MånedsinntektDto> månedsinntekter) {
}
