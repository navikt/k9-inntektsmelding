package no.nav.familie.inntektsmelding.imdialog.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record SendInntektsmeldingRequestDto(@Valid UUID foresporselUuid,
                                            @NotNull @Valid AktørIdDto aktorId,
                                            @NotNull @Valid YtelseTypeDto ytelse,
                                            @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                            @NotNull @Valid KontaktpersonRequestDto kontaktperson,
                                            @NotNull LocalDate startdato,
                                            @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                            @NotNull List<@Valid Refusjon> refusjon,
                                            @NotNull List<@Valid BortfaltNaturalytelseRequestDto> bortfaltNaturalytelsePerioder,
                                            @NotNull List<@Valid EndringsårsakerRequestDto> endringAvInntektÅrsaker,
                                            @Valid OmsorgspengerRequestDto omsorgspenger) {

    public record Refusjon(@NotNull LocalDate fom,
                           @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }


    public record BortfaltNaturalytelseRequestDto(@NotNull LocalDate fom,
                                                  LocalDate tom,
                                                  @NotNull NaturalytelsetypeDto naturalytelsetype,
                                                  @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record EndringsårsakerRequestDto(@NotNull @Valid EndringsårsakDto årsak,
                                            LocalDate fom,
                                            LocalDate tom,
                                            LocalDate bleKjentFom) {
    }

    public record KontaktpersonRequestDto(@Size(max = 100) @NotNull String navn,
                                          @NotNull @Size(max = 100) String telefonnummer) {
    }

    public record OmsorgspengerRequestDto(@NotNull Boolean harUtbetaltPliktigeDager,
                                          List<@Valid FraværHeleDagerRequestDto> fraværHeleDager,
                                          List<@Valid FraværDelerAvDagenRequestDto> fraværDelerAvDagen) {

        public record FraværHeleDagerRequestDto(@NotNull LocalDate fom,
                                                @NotNull LocalDate tom) {

            private boolean overlapper(FraværHeleDagerRequestDto annenFraværsPeriode) {
                return (fom.isBefore(annenFraværsPeriode.tom) || fom.isEqual(annenFraværsPeriode.tom)) &&
                       (tom.isAfter(annenFraværsPeriode.fom) || tom.isEqual(annenFraværsPeriode.fom));
            }

            private boolean inneholder(LocalDate dato) {
                return (fom.isBefore(dato) || fom.isEqual(dato)) && (tom.isAfter(dato) || tom.isEqual(dato));
            }
        }

        public record FraværDelerAvDagenRequestDto(@NotNull LocalDate dato,
                                                   @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 2, fraction = 2) BigDecimal timer) {
        }


        @AssertTrue(message = "Ingen fraværHeleDager overlapper")
        private boolean isValidIngenOverlappMellomFraværHeleDager() {
            if (fraværHeleDager == null || fraværHeleDager.isEmpty()) {
                return true;
            }
            return fraværHeleDager
                .stream()
                .noneMatch(fraværsPeriode -> fraværHeleDager
                    .stream()
                    .anyMatch(annenFraværsPeriode -> !annenFraværsPeriode.equals(fraværsPeriode) && annenFraværsPeriode.overlapper(fraværsPeriode)));
        }

        @AssertTrue(message = "Ingen duplikate fraværDelerAvDagen finnes")
        private boolean isValidIngenDuplikateFraværDelerAvDagen() {
            if (fraværDelerAvDagen == null || fraværDelerAvDagen.isEmpty()) {
                return true;
            }
            return fraværDelerAvDagen
                .stream()
                .map(FraværDelerAvDagenRequestDto::dato)
                .distinct()
                .count() == fraværDelerAvDagen.size();
        }

        @AssertTrue(message = "Ingen fraværDelerAvDagen finnes i fraværHeleDager")
        private boolean isValidIngenOvelerlappMellomFraværDelevAvDagenOgFraværHeleDager() {
            if (fraværHeleDager == null || fraværHeleDager.isEmpty() || fraværDelerAvDagen == null || fraværDelerAvDagen.isEmpty()) {
                return true;
            }
            return fraværHeleDager
                .stream()
                .noneMatch(fraværsPeriode -> fraværDelerAvDagen
                    .stream()
                    .anyMatch(fraværDelerAvDagen -> fraværsPeriode.inneholder(fraværDelerAvDagen.dato))
                );
        }


        @AssertTrue(message = "Må ha enten fraværHeleDager eller fraværDelerAvDagen")
        private boolean isValidFraværsPerioderOrDelvisFraværsPerioder() {
            if (fraværHeleDager == null || fraværHeleDager.isEmpty()) {
                return !(fraværDelerAvDagen == null || fraværDelerAvDagen.isEmpty());
            }
            return true;
        }

    }
}

