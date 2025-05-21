package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

public record OmsorgspengerDataDto(@Valid String begrunnelseForSøknad,
                                   @Valid List<FraværsPeriodeDto> fraværsPerioder,
                                   @Valid List<DelvisFraværsDagDto> delvisFraværsDager) {

    @AssertTrue(message = "Ingen fraværsPerioder overlapper")
    private boolean isValidIngenOverlappMellomfraværsPerioder() {
        if (fraværsPerioder == null || fraværsPerioder.isEmpty()) {
            return true;
        }
        return fraværsPerioder
            .stream()
            .noneMatch(fraværsPeriode -> fraværsPerioder
                .stream()
                .anyMatch(annenFraværsPeriode -> !annenFraværsPeriode.equals(fraværsPeriode) && annenFraværsPeriode.overlapper(fraværsPeriode)));
    }


    @AssertTrue(message = "Ingen duplikate delvisFraværsDager finnes")
    private boolean isValidIngenDuplikatedelvisFraværsDager() {
        if (delvisFraværsDager == null || delvisFraværsDager.isEmpty()) {
            return true;
        }
        return delvisFraværsDager
            .stream()
            .map(DelvisFraværsDagDto::dato)
            .distinct()
            .count() == delvisFraværsDager.size();
    }


    @AssertTrue(message = "Ingen delvisFraværsDager finnes i fraværsPerioder")
    private boolean isValidIngenOvelerlappMellomFraværDelevAvDagenOgfraværsPerioder() {
        if (fraværsPerioder == null || fraværsPerioder.isEmpty() || delvisFraværsDager == null || delvisFraværsDager.isEmpty()) {
            return true;
        }
        return fraværsPerioder
            .stream()
            .noneMatch(fraværsPeriode -> delvisFraværsDager
                .stream()
                .anyMatch(delvisFraværsDag -> fraværsPeriode.inneholder(delvisFraværsDag.dato()))
            );
    }


    @AssertTrue(message = "Må ha enten fraværsPerioder eller delvisFraværsDager")
    private boolean isValidFraværsPerioderOrDelvisFraværsPerioder() {
        if (fraværsPerioder == null || fraværsPerioder.isEmpty()) {
            return !(delvisFraværsDager == null || delvisFraværsDager.isEmpty());
        }
        return true;
    }
}
