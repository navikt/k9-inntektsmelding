package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static no.nav.familie.inntektsmelding.integrasjoner.dokgen.InntektsmeldingPdfData.formaterDatoNorsk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
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
            .medKontaktperson(mapKontaktperson(inntektsmelding))
            .medNaturalytelser(mapNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medIngenBortfaltNaturalytelse(erIngenBortalteNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medIngenGjenopptattNaturalytelse(erIngenGjenopptatteNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser()))
            .medRefusjonsendringer(mapRefusjonsendringPerioder(inntektsmelding.getRefusjonsendringer(), inntektsmelding.getOpphørsdatoRefusjon()));

        if (inntektsmelding.getMånedRefusjon() != null) {
            imDokumentdataBuilder.medRefusjonsbeløp(inntektsmelding.getMånedRefusjon());
        }
        if (inntektsmelding.getOpphørsdatoRefusjon().isBefore(Tid.TIDENES_ENDE)) {
            imDokumentdataBuilder.medRefusjonOpphørsdato(inntektsmelding.getOpphørsdatoRefusjon());
        }

        return imDokumentdataBuilder.build();
    }

    private static Kontaktperson mapKontaktperson(InntektsmeldingEntitet inntektsmelding) {
        if (Kildesystem.FPSAK.equals(inntektsmelding.getKildesystem())) {
            return new Kontaktperson(inntektsmelding.getOpprettetAv(), inntektsmelding.getOpprettetAv());
        } else {
            return new Kontaktperson(inntektsmelding.getKontaktperson().getNavn(), inntektsmelding.getKontaktperson().getTelefonnummer());
        }
    }

    private static boolean erIngenGjenopptatteNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalYtelser) {
        return naturalYtelser.isEmpty() || naturalYtelser.stream().filter(n -> n.getPeriode().getTom().isBefore(Tid.TIDENES_ENDE)).toList().isEmpty();
    }

    private static boolean erIngenBortalteNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalYtelser) {
        return naturalYtelser.isEmpty() || naturalYtelser.stream().filter(n -> n.getPeriode().getTom().isEqual(Tid.TIDENES_ENDE)).toList().isEmpty();
    }

    private static List<NaturalYtelse> mapNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalytelser) {
        List<NaturalYtelse> naturalytelserTilBrev = new ArrayList<>();

        naturalytelser.stream()
            .map(InntektsmeldingPdfDataMapper::opprettBortfalteNaturalytelserTilBrev)
            .forEach(naturalytelserTilBrev::add);

        naturalytelser.stream().filter(bn -> bn.getPeriode().getTom().isBefore(Tid.TIDENES_ENDE))
            .map(tilkommet -> opprettTilkomneNaturalytelserTilBrev(tilkommet, naturalytelser))
            .forEach(naturalytelserTilBrev::add);

        return naturalytelserTilBrev;
    }

    private static NaturalYtelse opprettBortfalteNaturalytelserTilBrev(BortaltNaturalytelseEntitet bn) {
        return new NaturalYtelse(formaterDatoNorsk(bn.getPeriode().getFom()),
            bn.getPeriode().getTom().equals(Tid.TIDENES_ENDE) ? null : formaterDatoNorsk(bn.getPeriode().getTom()),
            mapTypeTekst(bn.getType()),
            bn.getMånedBeløp(),
            true);
    }

    private static NaturalYtelse opprettTilkomneNaturalytelserTilBrev(BortaltNaturalytelseEntitet tilkommet, List<BortaltNaturalytelseEntitet> alleNaturalytelser) {
        var tomForTilkommet = finnNesteTomForTilkommet(tilkommet, alleNaturalytelser).orElse(null);

        return new NaturalYtelse(formaterDatoNorsk(tilkommet.getPeriode().getTom().plusDays(1)),
           tomForTilkommet != null ? formaterDatoNorsk(tomForTilkommet) : null,
            mapTypeTekst(tilkommet.getType()),
            tilkommet.getMånedBeløp(),
            false);

    }

    private static Optional <LocalDate> finnNesteTomForTilkommet(BortaltNaturalytelseEntitet tilkommet, List<BortaltNaturalytelseEntitet> alleNAturalytelser) {
       var nesteTom = alleNAturalytelser.stream()
            .filter(alleNaturalytelser -> alleNaturalytelser.getType().equals(tilkommet.getType())
                && alleNaturalytelser.getPeriode().getFom().isAfter(tilkommet.getPeriode().getTom()))
            .map(nesteTilkommet -> nesteTilkommet.getPeriode().getFom())
            .min(Comparator.naturalOrder());

       return nesteTom.map(d -> d.minusDays(1));
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

    private static List<RefusjonsendringPeriode> mapRefusjonsendringPerioder(List<RefusjonsendringEntitet> refusjonsendringer, LocalDate opphørsdatoRefusjon) {
        return refusjonsendringer.stream()
            .map(rpe -> new RefusjonsendringPeriode(formaterDatoNorsk(rpe.getFom()), formaterDatoNorsk(finnNesteFom(refusjonsendringer, rpe.getFom()).orElse(null)),
                rpe.getRefusjonPrMnd()))
            .toList();
    }

    private static Optional<LocalDate> finnNesteFom(List<RefusjonsendringEntitet> refusjonsendringer, LocalDate fom) {
        var nesteFom = refusjonsendringer.stream()
            .map(RefusjonsendringEntitet::getFom)
            .filter(reFom -> reFom.isAfter(fom))
            .min(Comparator.naturalOrder());
        return nesteFom.map(date -> date.minusDays(1));
    }
}
