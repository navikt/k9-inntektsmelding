package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public class ForespørselMapper {
    public static ForespørselEntitet mapForespørsel(String orgnummer, LocalDate skjæringstidspunkt, String aktørId, Ytelsetype ytelsetype, String saksnummer, LocalDate førsteUttaksdato) {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer(orgnummer)
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medAktørId(new AktørIdEntitet(aktørId))
            .medYtelseType(ytelsetype)
            .medSaksnummer(saksnummer)
            .medFørsteUttaksdato(førsteUttaksdato)
            .build();
    }
}
