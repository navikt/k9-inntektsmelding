package no.nav.familie.inntektsmelding.integrasjoner.dokgen;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.utils.FormatUtils;
import no.nav.familie.inntektsmelding.utils.mapper.NaturalYtelseMapper;
import no.nav.familie.inntektsmelding.utils.mapper.PdfDataMapperUtil;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingPdfDataMapper {

    private InntektsmeldingPdfDataMapper() {
        throw new IllegalStateException("InntektsmeldingPdfDataMapper: Utility class");
    }

    public static InntektsmeldingPdfRequest map(InntektsmeldingEntitet inntektsmelding,
                                                String arbeidsgiverNavn,
                                                PersonInfo personInfo,
                                                String arbeidsgvierIdent) {
        String avsenderSystem = "NAV_NO";
        String navnSøker = personInfo.mapNavn();
        String personnummer = FormatUtils.formaterPersonnummer(personInfo.fødselsnummer().getIdent());
        Ytelsetype ytelsetype = inntektsmelding.getYtelsetype();
        Kontaktperson kontaktperson = PdfDataMapperUtil.mapKontaktperson(inntektsmelding);
        LocalDate startDato = inntektsmelding.getStartDato();
        BigDecimal månedInntekt = inntektsmelding.getMånedInntekt();
        String opprettetTidspunkt = FormatUtils.formaterDatoOgTidNorsk(inntektsmelding.getOpprettetTidspunkt());

        List<RefusjonsendringPeriode> refusjonsendringer = finnRefusjonsendringPerioder(inntektsmelding, startDato);
        List<NaturalYtelse> naturalytelser = mapNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser());
        boolean ingenBortfaltNaturalytelse = erIngenBortalteNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser());
        boolean ingenGjenopptattNaturalytelse = erIngenGjenopptatteNaturalYtelser(inntektsmelding.getBorfalteNaturalYtelser());
        List<Endringsarsak> endringsarsaker = PdfDataMapperUtil.mapEndringsårsaker(inntektsmelding.getEndringsårsaker());
        int antallRefusjonsperioder = refusjonsendringer != null ? refusjonsendringer.size() : 0;

        return new InntektsmeldingPdfRequest(
            avsenderSystem,
            navnSøker,
            personnummer,
            ytelsetype,
            arbeidsgvierIdent,
            arbeidsgiverNavn,
            kontaktperson,
            FormatUtils.formaterDatoMedNavnPåUkedag(startDato),
            månedInntekt,
            opprettetTidspunkt,
            refusjonsendringer,
            naturalytelser,
            ingenBortfaltNaturalytelse,
            ingenGjenopptattNaturalytelse,
            endringsarsaker,
            antallRefusjonsperioder
        );
    }

    private static List<RefusjonsendringPeriode> finnRefusjonsendringPerioder(InntektsmeldingEntitet inntektsmelding, LocalDate startDato) {
        if (inntektsmelding.getMånedRefusjon() == null) {
            return null;
        }

        LocalDate opphørsdato = inntektsmelding.getOpphørsdatoRefusjon() != null ? inntektsmelding.getOpphørsdatoRefusjon() : null;
        return PdfDataMapperUtil.mapRefusjonsendringPerioder(inntektsmelding.getRefusjonsendringer(), opphørsdato, inntektsmelding.getMånedRefusjon(), startDato);
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
}
