package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record Endringsarsak(String arsak, String fom, String tom, String bleKjentFra) {
}
