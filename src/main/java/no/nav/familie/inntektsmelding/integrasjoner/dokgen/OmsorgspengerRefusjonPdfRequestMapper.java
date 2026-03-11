package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.utils.FormatUtils;
import no.nav.familie.inntektsmelding.utils.mapper.PdfDataMapperUtil;

public class OmsorgspengerRefusjonPdfRequestMapper {

    private OmsorgspengerRefusjonPdfRequestMapper() {
        throw new IllegalStateException("OmsorgspengerRefusjonPdfDataMapper: Utility class");
    }

    public static OmsorgspengerRefusjonPdfRequest map(InntektsmeldingEntitet inntektsmelding,
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
        Omsorgspenger omsorgspenger = mapOmsorgspenger(inntektsmelding.getOmsorgspenger());
        BigDecimal årForRefusjon = BigDecimal.valueOf(inntektsmelding.getStartDato().getYear());

        return new OmsorgspengerRefusjonPdfRequest(
            avsenderSystem,
            navnSøker,
            personnummer,
            arbeidsgiverIdent,
            arbeidsgiverNavn,
            kontaktperson,
            månedInntekt,
            opprettetTidspunkt,
            endringsarsaker,
            omsorgspenger,
            årForRefusjon
        );
    }

    private static Omsorgspenger mapOmsorgspenger(OmsorgspengerEntitet omsorgspenger) {
        if (omsorgspenger == null) {
            throw new IllegalStateException("InntektsmeldingEntitet mangler omsorgspenger data");
        }

        var fraværsPerioder = omsorgspenger.getFraværsPerioder()
            .stream()
            .map(fp -> new FraværsPeriode(
                FormatUtils.formaterDatoForLister(fp.getPeriode().getFom()),
                FormatUtils.formaterDatoForLister(fp.getPeriode().getTom())))
            .toList();

        var delvisFraværsPerioder = omsorgspenger.getDelvisFraværsPerioder()
            .stream()
            .filter(dfp -> dfp.getTimer().compareTo(BigDecimal.ZERO) > 0)
            .map(dfp -> new DelvisFraværsPeriode(FormatUtils.formaterDatoForLister(dfp.getDato()), dfp.getTimer()))
            .toList();

        var trukketFraværsPerioder = omsorgspenger.getDelvisFraværsPerioder()
            .stream()
            .filter(dfp -> dfp.getTimer().compareTo(BigDecimal.ZERO) == 0)
            .map(dfp -> new TrukketFraværsPeriode(FormatUtils.formaterDatoForLister(dfp.getDato())))
            .toList();

        return new Omsorgspenger(omsorgspenger.isHarUtbetaltPliktigeDager(), fraværsPerioder, delvisFraværsPerioder, trukketFraværsPerioder);
    }
}
