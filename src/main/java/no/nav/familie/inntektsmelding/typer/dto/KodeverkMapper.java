package no.nav.familie.inntektsmelding.typer.dto;

import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public class KodeverkMapper {

    public static NaturalytelseType mapNaturalytelseTilEntitet(NaturalytelsetypeDto dto) {
        return switch (dto) {
            case ELEKTRISK_KOMMUNIKASJON -> NaturalytelseType.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> NaturalytelseType.LOSJI;
            case KOST_DØGN -> NaturalytelseType.KOST_DØGN;
            case BESØKSREISER_HJEMMET_ANNET -> NaturalytelseType.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> NaturalytelseType.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> NaturalytelseType.RENTEFORDEL_LÅN;
            case BIL -> NaturalytelseType.BIL;
            case KOST_DAGER -> NaturalytelseType.KOST_DAGER;
            case BOLIG -> NaturalytelseType.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> NaturalytelseType.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> NaturalytelseType.FRI_TRANSPORT;
            case OPSJONER -> NaturalytelseType.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> NaturalytelseType.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> NaturalytelseType.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> NaturalytelseType.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> NaturalytelseType.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> NaturalytelseType.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> NaturalytelseType.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    public static Ytelsetype mapYtelsetype(YtelseTypeDto dto) {
        return switch (dto) {
            case FORELDREPENGER -> Ytelsetype.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelsetype.SVANGERSKAPSPENGER;
            case OMSORGSPENGER -> Ytelsetype.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> Ytelsetype.OPPLÆRINGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
        };
    }

    public static YtelseTypeDto mapYtelsetype(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
            case OMSORGSPENGER -> YtelseTypeDto.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> YtelseTypeDto.OPPLÆRINGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> YtelseTypeDto.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseTypeDto.PLEIEPENGER_NÆRSTÅENDE;
        };
    }

}
