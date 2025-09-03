package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;

public record HentInntektsopplysningerResponse(@Valid BigDecimal gjennomsnittLønn, @NotNull @Valid List<MånedsinntektDto> månedsinntekter) {
    public record MånedsinntektDto(@NotNull LocalDate fom, @NotNull LocalDate tom, BigDecimal beløp, @Valid @NotNull MånedslønnStatus status) {
    }
}
