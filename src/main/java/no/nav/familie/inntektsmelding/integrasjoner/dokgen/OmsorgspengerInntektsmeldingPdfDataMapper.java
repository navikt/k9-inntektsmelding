package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.utils.FormatUtils;
import no.nav.familie.inntektsmelding.utils.mapper.PdfDataMapperUtil;

public class OmsorgspengerInntektsmeldingPdfDataMapper {

    private OmsorgspengerInntektsmeldingPdfDataMapper() {
        throw new IllegalStateException("OmsorgspengerInntektsmeldingPdfDataMapper: Utility class");
    }

    public static OmsorgspengerInntektsmeldingPdfData mapOmsorgspengerInntektsmeldingData(InntektsmeldingEntitet inntektsmelding,
                                                                                          String arbeidsgiverNavn,
                                                                                          PersonInfo personInfo,
                                                                                          String arbeidsgvierIdent) {
        var imDokumentdataBuilder = new OmsorgspengerInntektsmeldingPdfData.Builder()
            .medNavn(personInfo.mapNavn())
            .medPersonnummer(personInfo.fødselsnummer().getIdent())
            .medArbeidsgiverIdent(arbeidsgvierIdent)
            .medArbeidsgiverNavn(arbeidsgiverNavn)
            .medAvsenderSystem("NAV_NO")
            .medOpprettetTidspunkt(inntektsmelding.getOpprettetTidspunkt())
            .medMånedInntekt(inntektsmelding.getMånedInntekt())
            .medKontaktperson(PdfDataMapperUtil.mapKontaktperson(inntektsmelding))
            .medEndringsårsaker(PdfDataMapperUtil.mapEndringsårsaker(inntektsmelding.getEndringsårsaker()))
            .medFraværsperioder(mapFraværsInfo(inntektsmelding.getOmsorgspenger()))
            .medHarUtbetaltLønn(mapHarUtbetaltLønn(inntektsmelding.getMånedRefusjon()));

        return imDokumentdataBuilder.build();
    }

    private static String mapHarUtbetaltLønn(BigDecimal månedRefusjon) {
        // Hvis arbeidsgiver har oppgitt refusjon, har de utbetalt lønn
        return månedRefusjon != null && månedRefusjon.compareTo(BigDecimal.ZERO) > 0 ? "Ja" : "Nei";
    }

    private static List<FraværsPeriode> mapFraværsInfo(OmsorgspengerEntitet omsorgspenger) {
        if (omsorgspenger == null || omsorgspenger.getFraværsPerioder() == null || omsorgspenger.getFraværsPerioder().isEmpty()) {
            throw new IllegalStateException("OmsorgspengerEntitet mangler fraværsperioder");
        }

        return omsorgspenger.getFraværsPerioder()
            .stream()
            .map(fp -> new FraværsPeriode(FormatUtils.formaterDatoForLister(fp.getPeriode().getFom()), FormatUtils.formaterDatoForLister(fp.getPeriode().getTom())))
            .toList();
    }
}
