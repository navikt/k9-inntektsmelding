package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record Inntektsopplysninger(BigDecimal gjennomsnitt, String orgnummer, List<InntektMåned> måneder) {
    public record InntektMåned(BigDecimal beløp, YearMonth månedÅr, MånedslønnStatus status){}
}
