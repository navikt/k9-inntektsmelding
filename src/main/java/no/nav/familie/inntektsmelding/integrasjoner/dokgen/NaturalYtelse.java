package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record NaturalYtelse(String fom, String naturalytelseType, BigDecimal beloep, boolean erBortfalt) {
}
