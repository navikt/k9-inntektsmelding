package no.nav.familie.inntektsmelding.imdialog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.familie.inntektsmelding.koder.Naturalytelsetype;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SendInntektsmeldingRequestDto(@NotNull @Valid InntektsmeldingDialogRest.AktørIdRequestDto aktorId, @NotNull @Valid Ytelsetype ytelse,
                                            @NotNull String arbeidsgiverIdent, @NotNull String telefonnummer, @NotNull LocalDate startdato,
                                            @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                            @NotNull List<@Valid RefusjonsperiodeRequestDto> refusjonsperioder,
                                            @NotNull List<@Valid NaturalytelseRequestDto> bortfaltNaturaltytelsePerioder) {
    public record RefusjonsperiodeRequestDto(@NotNull LocalDate fom, LocalDate tom,
                                             @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record NaturalytelseRequestDto(@NotNull LocalDate fom, LocalDate tom, @NotNull Naturalytelsetype naturalytelsetype,
                                          @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }
}
