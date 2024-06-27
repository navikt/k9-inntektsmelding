package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.math.BigDecimal;
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record NaturalYtelse(String fom, String tom, String naturalytelseType, BigDecimal beloep, boolean erBortfalt) {
}
