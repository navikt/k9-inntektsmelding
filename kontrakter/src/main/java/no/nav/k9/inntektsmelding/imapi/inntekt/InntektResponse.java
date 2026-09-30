package no.nav.k9.inntektsmelding.imapi.inntekt;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

public record InntektResponse(
    // NB: inntektPerMåned kan inneholde måneder med null-verdi (beløp ikke rapportert).
    // Global Jackson-konfig (JacksonJsonConfig) bruker NON_EMPTY inclusion, som ellers ville
    // fjernet slike map-entries fra JSON-responsen. @JsonInclude(content = ALWAYS) overstyrer
    // dette for map-verdiene slik at måneden fortsatt er til stede (med null) i kontrakten.
    @NotNull @JsonInclude(content = JsonInclude.Include.ALWAYS) Map<YearMonth, BigDecimal> inntektPerMåned,
    @NotNull BigDecimal gjennomsnitt) {
}
