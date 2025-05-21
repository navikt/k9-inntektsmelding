package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record DelvisFraværsDagDto(@NotNull LocalDate dato,
                                  @NotNull BigDecimal fravaersTimer,
                                  @NotNull BigDecimal forventetArbeidstimer) {
}
