package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.List;

import no.nav.familie.inntektsmelding.forespørsel.modell.DelvisFraværsDagForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.FraværsPeriodeForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.OmsorgspengerForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.DelvisFraværsDagDto;
import no.nav.familie.inntektsmelding.typer.dto.FraværsPeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.OmsorgspengerDataDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public class ForespørselMapper {
    public static ForespørselEntitet mapForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnummer, String saksnummer,
                                                    LocalDate førsteUttaksdato) {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer(orgnummer)
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medAktørId(new AktørIdEntitet(aktørId))
            .medYtelseType(ytelsetype)
            .medSaksnummer(saksnummer)
            .medFørsteUttaksdato(førsteUttaksdato)
            .build();
    }

    public static ForespørselEntitet mapForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnummer, String saksnummer,
                                                    LocalDate førsteUttaksdato, OmsorgspengerDataDto omsorgspenger) {
        return ForespørselEntitet.builder()
            .medOrganisasjonsnummer(orgnummer)
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medAktørId(new AktørIdEntitet(aktørId))
            .medYtelseType(ytelsetype)
            .medSaksnummer(saksnummer)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medOmsorgspenger(mapOmsorgspenger(omsorgspenger))
            .build();
    }

    public static OmsorgspengerForespørselEntitet mapOmsorgspenger(OmsorgspengerDataDto omsorgspenger) {
        return OmsorgspengerForespørselEntitet.builder()
            .medBegrunnelseForSøknad(omsorgspenger.begrunnelseForSøknad())
            .medFraværsPerioder(mapFraværsPerioder(omsorgspenger.fraværsPerioder()))
            .medDelvisFraværsDager(mapDelvisFraværsDager(omsorgspenger.delvisFraværsDager()))
            .build();
    }

    private static List<FraværsPeriodeForespørselEntitet> mapFraværsPerioder( List<FraværsPeriodeDto> fraværsPerioder) {
        if (fraværsPerioder == null) {
            return null;
        }
        return fraværsPerioder.stream()
            .map(fraværsPeriode -> new FraværsPeriodeForespørselEntitet(PeriodeEntitet.fraOgMedTilOgMed(fraværsPeriode.fom(), fraværsPeriode.tom())))
            .toList();
    }

    private static List<DelvisFraværsDagForespørselEntitet> mapDelvisFraværsDager( List<DelvisFraværsDagDto> delvisFraværsDagDtos) {
    if (delvisFraværsDagDtos == null) {
            return null;
        }
        return delvisFraværsDagDtos.stream()
            .map(delvisFraværsDag -> new DelvisFraværsDagForespørselEntitet(delvisFraværsDag.dato(), delvisFraværsDag.fraværstimer(), delvisFraværsDag.normalArbeidstid()))
            .toList();
    }
}
