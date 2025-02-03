package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HentInntektsopplysningerResponseDto(@Valid BigDecimal gjennomsnittLønn, @NotNull @Valid List<MånedsinntektDto> månedsinntekter) {
    public record MånedsinntektDto(@NotNull LocalDate fom, @NotNull LocalDate tom, BigDecimal beløp, @Valid @NotNull MånedslønnStatus status) {
    }
}
