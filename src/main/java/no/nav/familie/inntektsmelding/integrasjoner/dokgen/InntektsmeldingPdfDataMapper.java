package no.nav.familie.inntektsmelding.integrasjoner.dokgen;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.utils.FormatUtils;
import no.nav.familie.inntektsmelding.utils.mapper.NaturalYtelseMapper;
import no.nav.familie.inntektsmelding.utils.mapper.PdfDataMapperUtil;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingPdfDataMapper {

    private InntektsmeldingPdfDataMapper() {
        throw new IllegalStateException("InntektsmeldingPdfDataMapper: Utility class");
    }

    public static InntektsmeldingPdfData mapInntektsmeldingData(InntektsmeldingEntitet inntektsmelding,
                                                                String arbeidsgiverNavn,
                                                                PersonInfo personInfo,
                                                                String arbeidsgvierIdent) {
        var startdato = inntektsmelding.getStartDato();
        var imDokumentdataBuilder = new InntektsmeldingPdfData.Builder()
            .medNavn(personInfo.mapNavn())
            .medPersonnummer(personInfo.fødselsnummer().getIdent())
            .medArbeidsgiverIdent(arbeidsgvierIdent)
            .medArbeidsgiverNavn(arbeidsgiverNavn)
            .medAvsenderSystem("NAV_NO")
            .medYtelseNavn(inntektsmelding.getYtelsetype())
            .medOpprettetTidspunkt(inntektsmelding.getOpprettetTidspunkt())
            .medStartDato(startdato)
            .medMånedInntekt(inntektsmelding.getMånedInntekt())
            .medKontaktperson(PdfDataMapperUtil.mapKontaktperson(inntektsmelding))
            .medNaturalytelser(mapNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medIngenBortfaltNaturalytelse(erIngenBortalteNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medIngenGjenopptattNaturalytelse(erIngenGjenopptatteNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medEndringsårsaker(PdfDataMapperUtil.mapEndringsårsaker(inntektsmelding.getEndringsårsaker()));

        if (inntektsmelding.getMånedRefusjon() != null) {
            var opphørsdato = inntektsmelding.getOpphørsdatoRefusjon() != null ? inntektsmelding.getOpphørsdatoRefusjon() : null;
            var refusjonsendringerTilPdf = mapRefusjonsendringPerioder(inntektsmelding.getRefusjonsendringer(), opphørsdato, inntektsmelding.getMånedRefusjon(), startdato);
            imDokumentdataBuilder.medRefusjonsendringer(refusjonsendringerTilPdf);
            imDokumentdataBuilder.medAntallRefusjonsperioder(refusjonsendringerTilPdf.size());
        } else {
            imDokumentdataBuilder.medAntallRefusjonsperioder(0);
        }

        return imDokumentdataBuilder.build();
    }

    private static boolean erIngenGjenopptatteNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalYtelser) {
        return naturalYtelser.isEmpty() || naturalYtelser.stream().filter(n -> n.getPeriode().getTom().isBefore(Tid.TIDENES_ENDE)).toList().isEmpty();
    }

    private static boolean erIngenBortalteNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalYtelser) {
        return naturalYtelser.isEmpty();
    }

    private static List<NaturalYtelse> mapNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalytelser) {
        return NaturalYtelseMapper.mapNaturalYtelser(naturalytelser).stream()
            .map(InntektsmeldingPdfDataMapper::opprettNaturalytelserTilBrev)
            .toList();
    }

    private static NaturalYtelse opprettNaturalytelserTilBrev(NaturalYtelseMapper.NaturalYtelse bn) {
        return new NaturalYtelse(FormatUtils.formaterDatoForLister(bn.fom()),
            mapTypeTekst(bn.type()),
            bn.beløp(),
            bn.bortfallt());
    }

    private static String mapTypeTekst(NaturalytelseType type) {
        return switch (type) {
            case ELEKTRISK_KOMMUNIKASJON -> "Elektrisk kommunikasjon";
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> "Aksjer grunnfondsbevis til underkurs";
            case LOSJI -> "Losji";
            case KOST_DOEGN -> "Kostpenger døgnsats";
            case BESØKSREISER_HJEMMET_ANNET -> "Besøksreiser hjemmet annet";
            case KOSTBESPARELSE_I_HJEMMET -> "Kostbesparelser i hjemmet";
            case RENTEFORDEL_LÅN -> "Rentefordel lån";
            case BIL -> "Bil";
            case KOST_DAGER -> "Kostpenger dager";
            case BOLIG -> "Bolig";
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> "Skattepliktig del forsikringer";
            case FRI_TRANSPORT -> "Fri transport";
            case OPSJONER -> "Opsjoner";
            case TILSKUDD_BARNEHAGEPLASS -> "Tilskudd barnehageplass";
            case ANNET -> "Annet";
            case BEDRIFTSBARNEHAGEPLASS -> "Bedriftsbarnehageplass";
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> "Yrkesbil tjenesteligbehov kilometer";
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> "Yrkesbil tjenesteligbehov listepris";
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> "Innbetaling utenlandsk pensjonsordning";
        };
    }

    private static List<RefusjonsendringPeriode> mapRefusjonsendringPerioder(List<RefusjonsendringEntitet> refusjonsendringer,
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
