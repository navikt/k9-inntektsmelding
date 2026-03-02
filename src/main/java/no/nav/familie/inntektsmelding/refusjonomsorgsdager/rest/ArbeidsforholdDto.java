package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import java.time.LocalDate;

public record ArbeidsforholdDto(String organisasjonsnummer,
                                String arbeidsforholdId, // TODO: Sjekk om dette er i bruk. Hvis ikke slett.
                                Ansettelsesperiode ansettelsesperiode) {
    public record Ansettelsesperiode (LocalDate fom, LocalDate tom) {}
}
