package no.nav.familie.inntektsmelding.integrasjoner.aareg.dto;

import java.util.List;

public record ArbeidsforholdDto(
    String arbeidsforholdId,
    Long navArbeidsforholdId,
    OpplysningspliktigArbeidsgiverDto arbeidsgiver,
    AnsettelsesperiodeDto ansettelsesperiode,
    List<ArbeidsavtaleDto> arbeidsavtaler,
    List<PermisjonPermitteringDto> permisjonPermitteringer,
    String type // (kodeverk: Arbeidsforholdtyper)
) {
    public List<ArbeidsavtaleDto> arbeidsavtaler() {
        return arbeidsavtaler != null ? arbeidsavtaler : List.of();
    }

    public List<PermisjonPermitteringDto> permisjonPermitteringer() {
        return permisjonPermitteringer != null ? permisjonPermitteringer : List.of();
    }
}
