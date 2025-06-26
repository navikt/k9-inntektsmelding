package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.utils.mapper.PdfDataMapperUtil;

public class OmsorgspengerRefusjonPdfDataMapper {

    private OmsorgspengerRefusjonPdfDataMapper() {
        throw new IllegalStateException("OmsorgspengerPdfDataMapper: Utility class");
    }

    public static OmsorgspengerRefusjonPdfData mapOmsorgspengerRefusjonData(InntektsmeldingEntitet inntektsmelding,
                                                                            String arbeidsgiverNavn,
                                                                            PersonInfo personInfo,
                                                                            String arbeidsgvierIdent) {
        if (inntektsmelding.getOmsorgspenger() == null) {
            throw new IllegalStateException("InntektsmeldingEntitet mangler omsorgspenger data");
        }

        var startdato = inntektsmelding.getStartDato();
        var imDokumentdataBuilder = new OmsorgspengerRefusjonPdfData.Builder()
            .medNavn(personInfo.mapNavn())
            .medPersonnummer(personInfo.fødselsnummer().getIdent())
            .medArbeidsgiverIdent(arbeidsgvierIdent)
            .medArbeidsgiverNavn(arbeidsgiverNavn)
            .medAvsenderSystem("NAV_NO")
            .medOpprettetTidspunkt(inntektsmelding.getOpprettetTidspunkt())
            .medMånedInntekt(inntektsmelding.getMånedInntekt())
            .medKontaktperson(PdfDataMapperUtil.mapKontaktperson(inntektsmelding))
            .medEndringsårsaker(PdfDataMapperUtil.mapEndringsårsaker(inntektsmelding.getEndringsårsaker()))
            .medOmsorgspenger(mapOmsorgspenger(inntektsmelding.getOmsorgspenger()))
            .medÅrForRefusjon(BigDecimal.valueOf(startdato.getYear()));

        return imDokumentdataBuilder.build();
    }

    private static Omsorgspenger mapOmsorgspenger(OmsorgspengerEntitet omsorgspenger) {
        var datoFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        var fraværsPerioder = omsorgspenger.getFraværsPerioder()
            .stream()
            .map(fp -> new Omsorgspenger.FraværsPeriode(fp.getPeriode().getFom().format(datoFormat), fp.getPeriode().getTom().format(datoFormat)))
            .toList();

        var delvisFraværsPerioder = omsorgspenger.getDelvisFraværsPerioder()
            .stream()
            .filter(dfp -> dfp.getTimer().compareTo(BigDecimal.ZERO) > 0)
            .map(dfp -> new Omsorgspenger.DelvisFraværsPeriode(dfp.getDato().format(datoFormat), dfp.getTimer()))
            .toList();

        var trukketFraværsPerioder = omsorgspenger.getDelvisFraværsPerioder()
            .stream()
            .filter(dfp -> dfp.getTimer().compareTo(BigDecimal.ZERO) == 0)
            .map(dfp -> new Omsorgspenger.TrukketFraværsPeriode(dfp.getDato().format(datoFormat)))
            .toList();

        return new Omsorgspenger(omsorgspenger.isHarUtbetaltPliktigeDager(), fraværsPerioder, delvisFraværsPerioder, trukketFraværsPerioder);
    }
}
