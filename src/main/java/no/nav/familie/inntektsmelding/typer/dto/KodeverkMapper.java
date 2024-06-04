package no.nav.familie.inntektsmelding.typer.dto;

import no.nav.familie.inntektsmelding.koder.Naturalytelsetype;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public class KodeverkMapper {

    public static Naturalytelsetype mapNaturalytelseTilEntitet(NaturalytelsetypeDto dto) {
        return switch (dto) {
            case ELEKTRISK_KOMMUNIKASJON -> Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> Naturalytelsetype.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> Naturalytelsetype.LOSJI;
            case KOST_DØGN -> Naturalytelsetype.KOST_DØGN;
            case BESØKSREISER_HJEMMET_ANNET -> Naturalytelsetype.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> Naturalytelsetype.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> Naturalytelsetype.RENTEFORDEL_LÅN;
            case BIL -> Naturalytelsetype.BIL;
            case KOST_DAGER -> Naturalytelsetype.KOST_DAGER;
            case BOLIG -> Naturalytelsetype.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> Naturalytelsetype.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> Naturalytelsetype.FRI_TRANSPORT;
            case OPSJONER -> Naturalytelsetype.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> Naturalytelsetype.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> Naturalytelsetype.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> Naturalytelsetype.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> Naturalytelsetype.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> Naturalytelsetype.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> Naturalytelsetype.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    public static Ytelsetype mapYtelsetypeTilEntitet(YtelseTypeDto dto) {
        return switch (dto) {
            case FORELDREPENGER -> Ytelsetype.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelsetype.SVANGERSKAPSPENGER;
            case OMSORGSPENGER -> Ytelsetype.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> Ytelsetype.OPPLÆRINGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
        };
    }
}
