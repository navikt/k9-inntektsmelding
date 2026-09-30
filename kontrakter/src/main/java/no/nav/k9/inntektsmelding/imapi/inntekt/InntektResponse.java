package no.nav.k9.inntektsmelding.imapi.inntekt;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record InntektResponse(@NotNull @JsonInclude(content = JsonInclude.Include.ALWAYS) Map<YearMonth, BigDecimal> inntektPerMåned,
                              @NotNull BigDecimal gjennomsnitt) {
}
