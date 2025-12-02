package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.utils.FormatUtils;
import no.nav.familie.inntektsmelding.utils.mapper.PdfDataMapperUtil;

public class OmsorgspengerInntektsmeldingPdfRequestMapper {

    private OmsorgspengerInntektsmeldingPdfRequestMapper() {
        throw new IllegalStateException("OmsorgspengerPdfRequest: Utility class");
    }

    public static OmsorgspengerInntektsmeldingPdfRequest map(InntektsmeldingEntitet inntektsmelding,
                                                             String arbeidsgiverNavn,
                                                             PersonInfo personInfo,
                                                             String arbeidsgiverIdent) {
        String avsenderSystem = "NAV_NO";
        String navnSøker = personInfo.mapNavn();
        String personnummer = FormatUtils.formaterPersonnummer(personInfo.fødselsnummer().getIdent());
        Kontaktperson kontaktperson = PdfDataMapperUtil.mapKontaktperson(inntektsmelding);
        BigDecimal månedInntekt = inntektsmelding.getMånedInntekt();
        String opprettetTidspunkt = FormatUtils.formaterDatoOgTidNorsk(inntektsmelding.getOpprettetTidspunkt());
        List<Endringsarsak> endringsarsaker = PdfDataMapperUtil.mapEndringsårsaker(inntektsmelding.getEndringsårsaker());
        List<FraværsPeriode> fraværsperioder = mapFraværsInfo(inntektsmelding.getOmsorgspenger());
        String harUtbetaltLønn = mapHarUtbetaltLønn(inntektsmelding.getMånedRefusjon());

        return new OmsorgspengerInntektsmeldingPdfRequest(
            avsenderSystem,
            navnSøker,
            personnummer,
            arbeidsgiverIdent,
            arbeidsgiverNavn,
            kontaktperson,
            månedInntekt,
            opprettetTidspunkt,
            endringsarsaker,
            fraværsperioder,
            harUtbetaltLønn
        );
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
