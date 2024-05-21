package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.time.YearMonth;

public record FinnInntektRequest(String aktørId, YearMonth fom, YearMonth tom) {}

