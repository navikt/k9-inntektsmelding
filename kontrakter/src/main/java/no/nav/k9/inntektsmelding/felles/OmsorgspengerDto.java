package no.nav.k9.inntektsmelding.felles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OmsorgspengerDto(@NotNull Boolean harUtbetaltPliktigeDager,
                               List<@Valid FraværHeleDagerDto> fraværHeleDager,
                               List<@Valid FraværDelerAvDagenDto> fraværDelerAvDagen) {

    public record FraværDelerAvDagenDto(@NotNull LocalDate dato,
                                        @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 2, fraction = 2) BigDecimal timer) {
    }

    public record FraværHeleDagerDto(@NotNull LocalDate fom,
                                     @NotNull LocalDate tom) {

        @AssertTrue(message = "fom er før eller lik tom")
        private boolean isValidFomErFørTom() {
            return fom.isBefore(tom) || fom.isEqual(tom);
        }

        private boolean overlapper(FraværHeleDagerDto annen) {
            return (fom.isBefore(annen.tom) || fom.isEqual(annen.tom)) &&
                (tom.isAfter(annen.fom) || tom.isEqual(annen.fom));
        }

        private boolean inneholder(LocalDate dato) {
            return (fom.isBefore(dato) || fom.isEqual(dato)) && (tom.isAfter(dato) || tom.isEqual(dato));
        }
    }

    @AssertTrue(message = "Ingen fraværHeleDager overlapper")
    private boolean isValidIngenOverlappMellomFraværHeleDager() {
        if (fraværHeleDager == null || fraværHeleDager.isEmpty()) {
            return true;
        }
        return fraværHeleDager.stream()
            .noneMatch(periode -> fraværHeleDager.stream()
                .anyMatch(annen -> !annen.equals(periode) && annen.overlapper(periode)));
    }

    @AssertTrue(message = "Ingen duplikate fraværDelerAvDagen finnes")
    private boolean isValidIngenDuplikateFraværDelerAvDagen() {
        if (fraværDelerAvDagen == null || fraværDelerAvDagen.isEmpty()) {
            return true;
        }
        return fraværDelerAvDagen.stream()
            .map(FraværDelerAvDagenDto::dato)
            .distinct()
            .count() == fraværDelerAvDagen.size();
    }

    @AssertTrue(message = "Ingen fraværDelerAvDagen finnes i fraværHeleDager")
    private boolean isValidIngenOverlappMellomFraværDelerAvDagenOgHeleDager() {
        if (fraværHeleDager == null || fraværHeleDager.isEmpty() || fraværDelerAvDagen == null || fraværDelerAvDagen.isEmpty()) {
            return true;
        }
        return fraværHeleDager.stream()
            .noneMatch(heldag -> fraværDelerAvDagen.stream()
                .anyMatch(delvis -> heldag.inneholder(delvis.dato())));
    }

    @AssertTrue(message = "Må ha enten fraværHeleDager eller fraværDelerAvDagen")
    private boolean isValidHarFraværsperioder() {
        if (fraværHeleDager == null || fraværHeleDager.isEmpty()) {
            return !(fraværDelerAvDagen == null || fraværDelerAvDagen.isEmpty());
        }
        return true;
    }
}
