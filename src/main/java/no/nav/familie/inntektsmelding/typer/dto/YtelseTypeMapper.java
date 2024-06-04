package no.nav.familie.inntektsmelding.typer.dto;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public class YtelseTypeMapper {
    public static Ytelsetype map(YtelseTypeDto ytelseTypeDto) {
        return switch (ytelseTypeDto) {
            case FORELDREPENGER -> Ytelsetype.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelsetype.SVANGERSKAPSPENGER;
            case OMSORGSPENGER -> Ytelsetype.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> Ytelsetype.OPPLÆRINGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
        };
    }
}

