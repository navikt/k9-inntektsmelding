package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record Inntektsopplysninger(BigDecimal gjennomsnitt, String orgnummer, List<InntektMåned> måneder) {
    public enum LønnStatus {
        BRUKT_I_GJENNOMSNITT,
        IKKE_RAPPORTERT,
        RAPPORTERINGSFRIST_IKKE_PASSERT
    }
    public record InntektMåned(BigDecimal beløp, YearMonth månedÅr, LønnStatus status){}
}
