package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoNorsk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.NaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonPeriodeEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingPdfDataMapper {
    public static InntektsmeldingPdfData mapInntektsmeldingData(InntektsmeldingEntitet inntektsmelding,
                                                                String arbeidsgiverNavn,
                                                                PersonInfo personInfo,
                                                                String arbeidsgvierIdent) {
        var imDokumentdataBuilder = new InntektsmeldingPdfData.Builder()
            .medNavn(personInfo.mapNavn())
            .medForNavnSøker(personInfo.mapFornavn())
            .medPersonnummer(personInfo.fødselsnummer().getIdent())
            .medArbeidsgiverIdent(arbeidsgvierIdent)
            .medArbeidsgiverNavn(arbeidsgiverNavn)
            .medAvsenderSystem("NAV_NO")
            .medYtelseNavn(inntektsmelding.getYtelsetype())
            .medOpprettetTidspunkt(inntektsmelding.getOpprettetTidspunkt())
            .medStartDato(inntektsmelding.getStartDato())
            .medMånedInntekt(inntektsmelding.getMånedInntekt())
            .medKontaktperson(mapKontaktperson(inntektsmelding.getKontaktperson()))
            .medNaturalytelser(mapNauralYtelser(inntektsmelding.getNaturalYtelser()))
            .medIngenBortfaltNaturalytelse(erIngenBortalteNaturalYtelser(inntektsmelding.getNaturalYtelser()))
            .medIngenGjenopptattNaturalytelse(erIngenGjenopptatteNaturalYtelser(inntektsmelding.getNaturalYtelser()));

        //Refusjon
        var startDato = inntektsmelding.getStartDato();
        utledRefusjonsbeløp(inntektsmelding.getRefusjonsPerioder(), startDato).ifPresent(imDokumentdataBuilder::medRefusjonsbeløp);
        imDokumentdataBuilder.medEndringIRefusjonsperioder(mapEndringIRefusjonsperioder(inntektsmelding.getRefusjonsPerioder(), startDato));
        utledOpphørsdato(inntektsmelding.getRefusjonsPerioder()).ifPresent(imDokumentdataBuilder::medRefusjonOpphørsdato);

        return imDokumentdataBuilder.build();
    }

    private static Kontaktperson mapKontaktperson(KontaktpersonEntitet kontaktpersonEntitet) {
        return new Kontaktperson(kontaktpersonEntitet.getNavn(), kontaktpersonEntitet.getTelefonnummer());
    }

    private static Optional<LocalDate> utledOpphørsdato(List<RefusjonPeriodeEntitet> refusjonsPerioder) {
        var sisteTomRefusjon = refusjonsPerioder.stream().map(rp -> rp.getPeriode().getTom()).max(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);
        return !Tid.TIDENES_ENDE.equals(sisteTomRefusjon) ? Optional.of(sisteTomRefusjon) : Optional.empty();
    }

    private static boolean erIngenGjenopptatteNaturalYtelser(List<NaturalytelseEntitet> naturalYtelser) {
        return naturalYtelser.isEmpty() || naturalYtelser.stream().allMatch(NaturalytelseEntitet::getErBortfalt);
    }

    private static boolean erIngenBortalteNaturalYtelser(List<NaturalytelseEntitet> naturalYtelser) {
        return naturalYtelser.isEmpty() || naturalYtelser.stream().noneMatch(NaturalytelseEntitet::getErBortfalt);
    }

    private static Optional<BigDecimal> utledRefusjonsbeløp(List<RefusjonPeriodeEntitet> refusjonsPerioder, LocalDate startDato) {
        return refusjonsPerioder.stream()
            .filter(refusjonPeriodeEntitet -> (startDato).equals(refusjonPeriodeEntitet.getPeriode().getFom()))
            .findFirst()
            .map(RefusjonPeriodeEntitet::getBeløp);
    }

    private static List<NaturalYtelse> mapNauralYtelser(List<NaturalytelseEntitet> naturalytelser) {
        return naturalytelser.stream()
            .map(ny -> new NaturalYtelse(formaterDatoNorsk(ny.getPeriode().getFom()), formaterDatoNorsk(ny.getPeriode().getTom()),
                mapTypeTekst(ny.getType()), ny.getBeløp(), ny.getErBortfalt()))
            .toList();
    }

    private static String mapTypeTekst(NaturalytelseType type) {
        return switch (type) {
            case ELEKTRISK_KOMMUNIKASJON -> "Elektrisk kommunikasjon";
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> "Aksjer grunnfondsbevis til underkurs";
            case LOSJI -> "Losji";
            case KOST_DØGN -> "Kostpenger døgnsats";
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

    private static List<RefusjonPeriode> mapEndringIRefusjonsperioder(List<RefusjonPeriodeEntitet> refusjonsPerioder, LocalDate startDato) {
        return refusjonsPerioder.stream()
            .filter(refusjonsPeriode -> refusjonsPeriode.getPeriode().getFom().isAfter(startDato))
            .map(rpe -> new RefusjonPeriode(formaterDatoNorsk(rpe.getPeriode().getFom()), formaterDatoNorsk(rpe.getPeriode().getTom()),
                rpe.getBeløp()))
            .toList();
    }
}
