package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record Omsorgspenger(@NotNull Boolean harUtbetaltPliktigeDager,
                            List<FraværsPeriode> fraværsPerioder,
                            List<DelvisFraværsPeriode> delvisFraværsPerioder,
                            List<TrukketFraværsPeriode> trukketFraværsPerioder) {
}
