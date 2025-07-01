package no.nav.familie.inntektsmelding.utils.mapper;

import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.Endringsarsak;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.Kontaktperson;

import java.util.List;

import static no.nav.familie.inntektsmelding.utils.FormatUtils.formaterDatoForLister;

public final class PdfDataMapperUtil {

    private PdfDataMapperUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Endringsarsak> mapEndringsårsaker(List<EndringsårsakEntitet> endringsårsaker) {
        return endringsårsaker.stream()
            .map(endringsårsakEntitet -> new Endringsarsak(
                endringsårsakEntitet.getÅrsak().getBeskrivelse(),
                formaterDatoForLister(endringsårsakEntitet.getFom().orElse(null)),
                formaterDatoForLister(endringsårsakEntitet.getTom().orElse(null)),
                formaterDatoForLister(endringsårsakEntitet.getBleKjentFom().orElse(null))))
            .toList();
    }

    public static Kontaktperson mapKontaktperson(InntektsmeldingEntitet inntektsmelding) {
        return new Kontaktperson(
            inntektsmelding.getKontaktperson().getNavn(),
            inntektsmelding.getKontaktperson().getTelefonnummer());
    }
}
