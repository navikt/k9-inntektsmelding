package no.nav.familie.inntektsmelding.imdialog.rest;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MånedsinntektResponsDto(LocalDate fom, LocalDate tom, BigDecimal beløp, String organisasjonsnummer) {
}
