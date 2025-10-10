package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;
import java.util.List;

import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public class ForespørselMapper {
    public static ForespørselEntitet mapForespørsel(String orgnummer,
                                                    LocalDate skjæringstidspunkt,
                                                    String aktørId,
                                                    Ytelsetype ytelsetype,
                                                    String saksnummer,
                                                    ForespørselType forespørselType,
                                                    LocalDate førsteUttaksdato,
                                                    List<PeriodeDto> etterspurtePerioder) {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer(orgnummer)
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medAktørId(new AktørIdEntitet(aktørId))
            .medYtelseType(ytelsetype)
            .medSaksnummer(saksnummer)
            .medForespørselType(forespørselType)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medEtterspurtePerioder(etterspurtePerioder)
            .build();
    }
}
