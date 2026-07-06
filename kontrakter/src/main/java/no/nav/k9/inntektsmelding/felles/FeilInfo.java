package no.nav.k9.inntektsmelding.felles;

public record FeilInfo(FeilkodeDto feilkode,
                       String feilmelding,
                       String referanseId) {}
