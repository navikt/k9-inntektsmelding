package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import jakarta.validation.constraints.NotNull;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record Omsorgspenger(@NotNull Boolean harUtbetaltPliktigeDager,
                            List<FraværsPeriode> fraværsPerioder,
                            List<DelvisFraværsPeriode> delvisFraværsPerioder) {

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public record FraværsPeriode(@NotNull String fom,
                                 @NotNull String tom) {

    }

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public record DelvisFraværsPeriode(@NotNull String dato,
                                       @NotNull BigDecimal timer) {
    }
}
