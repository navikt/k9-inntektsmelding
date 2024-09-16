package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

public record RefusjonsendringPeriode(String fom, String tom, BigDecimal beloep) {

}
