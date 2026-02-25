package no.nav.familie.inntektsmelding.integrasjoner.k9sak;

import java.util.List;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.k9.sak.typer.AktørId;

public record FagsakInfo(SaksnummerDto saksnummer,
                         Ytelsetype ytelseType,
                         AktørId aktørId,
                         PeriodeDto gyldigPeriode,
                         List<PeriodeDto> søknadsPerioder,
                         boolean venterForTidligSøknad) {
}
