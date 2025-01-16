package no.nav.familie.inntektsmelding.integrasjoner.aareg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArbeidsforholdDto(
    String id,
    Long navArbeidsforholdId,
    OpplysningspliktigArbeidsgiverDto arbeidsgiver,
    AnsettelsesperiodeDto ansettelsesperiode,
    String type // (kodeverk: Arbeidsforholdtyper)
) {

    public record AnsettelsesperiodeDto(LocalDate startdato, LocalDate sluttdato) { }

    public record OpplysningspliktigArbeidsgiverDto(Type type, List<Ident> identer) {
        public record Ident(Type type, String ident, Boolean gjeldende) {
            public enum Type {
                AKTORID, FOLKEREGISTERIDENT, ORGANISASJONSNUMMER
            }
        }

        public enum Type {
            Organisasjon,
            Person
        }
    }
}
