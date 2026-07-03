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
                               List<@Valid PeriodeDto> fraværHeleDager,
                               List<@Valid FraværDelerAvDagenDto> fraværDelerAvDagen,
                               List<@Valid PeriodeDto> trukketPerioder) {

    public record FraværDelerAvDagenDto(@NotNull LocalDate dato,
                                        @NotNull @Min(0) @Max(24) @Digits(integer = 2, fraction = 2) BigDecimal timer) {
    }

    @AssertTrue(message = "Ingen fraværHeleDager overlapper")
    private boolean isValidIngenOverlappMellomFraværHeleDager() {
        if (fraværHeleDager == null || fraværHeleDager.isEmpty()) {
            return true;
        }
        return fraværHeleDager.stream()
            .noneMatch(periode -> fraværHeleDager.stream()
                .anyMatch(annen -> annen != periode && annen.overlapper(periode)));
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
                .anyMatch(delvis -> heldag.inneholderDato(delvis.dato())));
    }

    @AssertTrue(message = "Ingen dato i trukketPerioder kan finnes i fraværHeleDager eller fraværDelerAvDagen")
    private boolean isValidIngenOverlappTrukketPerioder() {
        if (trukketPerioder == null || trukketPerioder.isEmpty()) {
            return true;
        }
        if (fraværHeleDager != null) {
            for (PeriodeDto trukket : trukketPerioder) {
                if (fraværHeleDager.stream().anyMatch(heldag -> heldag.overlapper(trukket))) {
                    return false;
                }
            }
        }
        if (fraværDelerAvDagen != null) {
            for (PeriodeDto trukket : trukketPerioder) {
                if (fraværDelerAvDagen.stream().anyMatch(delvis -> trukket.inneholderDato(delvis.dato()))) {
                    return false;
                }
            }
        }
        return true;
    }

    @AssertTrue(message = "Må ha enten fraværHeleDager, fraværDelerAvDagen eller trukketPerioder")
    private boolean isValidHarFraværsperioder() {
        boolean harFraværHeleDager = fraværHeleDager != null && !fraværHeleDager.isEmpty();
        boolean harFraværDelerAvDagen = fraværDelerAvDagen != null && !fraværDelerAvDagen.isEmpty();
        boolean harTrukketPerioder = trukketPerioder != null && !trukketPerioder.isEmpty();
        return harFraværHeleDager || harFraværDelerAvDagen || harTrukketPerioder;
    }
}
