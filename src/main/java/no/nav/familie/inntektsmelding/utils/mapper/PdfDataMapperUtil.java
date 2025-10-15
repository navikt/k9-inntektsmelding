package no.nav.familie.inntektsmelding.utils.mapper;

import static no.nav.familie.inntektsmelding.utils.FormatUtils.formaterDatoForLister;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.Endringsarsak;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.Kontaktperson;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.RefusjonsendringPeriode;
import no.nav.familie.inntektsmelding.utils.FormatUtils;
import no.nav.vedtak.konfig.Tid;

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

    public static List<RefusjonsendringPeriode> mapRefusjonsendringPerioder(List<RefusjonsendringEntitet> refusjonsendringer,
                                                                             LocalDate opphørsdato,
                                                                             BigDecimal refusjonsbeløp,
                                                                             LocalDate startdato) {
        List<RefusjonsendringPeriode> refusjonsendringerTilBrev = new ArrayList<>();

        //første perioden
        refusjonsendringerTilBrev.add(new RefusjonsendringPeriode(FormatUtils.formaterDatoForLister(startdato), startdato, refusjonsbeløp));

        refusjonsendringerTilBrev.addAll(
            refusjonsendringer.stream().map(rpe -> new RefusjonsendringPeriode(FormatUtils.formaterDatoForLister(rpe.getFom()), rpe.getFom(), rpe.getRefusjonPrMnd()))
                .toList());

        if (opphørsdato != null && !opphørsdato.equals(Tid.TIDENES_ENDE)) {
            // Da opphørsdato er siste dag med refusjon må vi legge til denne mappingen for å få det rett ut i PDF, da vi ønsker å vise når første dag uten refusjon er
            refusjonsendringerTilBrev.add(new RefusjonsendringPeriode(FormatUtils.formaterDatoForLister(opphørsdato.plusDays(1)), opphørsdato.plusDays(1), BigDecimal.ZERO));
        }

        return refusjonsendringerTilBrev.stream()
            .sorted(Comparator.comparing(RefusjonsendringPeriode::fraDato))
            .toList();
    }
}
