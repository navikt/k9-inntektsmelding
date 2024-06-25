package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;

public record NaturalYtelse(String fom, String tom, String type, BigDecimal beloep, boolean erBortfalt) {
}
