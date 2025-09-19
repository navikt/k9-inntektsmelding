package no.nav.familie.inntektsmelding.integrasjoner.k9sak;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;

public record FagsakInfo(SaksnummerDto saksnummer,
                         Ytelsetype ytelseType,
                         PeriodeDto gyldigPeriode) {
}
