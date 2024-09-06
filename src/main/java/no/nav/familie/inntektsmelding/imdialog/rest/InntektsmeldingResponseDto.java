package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InntektsmeldingResponseDto(
    @NotNull @Valid UUID foresporselUuid, @NotNull @Valid AktørIdDto aktorId,
    @NotNull @Valid YtelseTypeDto ytelse, @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
    @NotNull @Valid SendInntektsmeldingRequestDto.KontaktpersonRequestDto kontaktperson, @NotNull LocalDate startdato,
    @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
    @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal refusjon,
    @NotNull LocalDateTime opprettetTidspunkt,
    @NotNull List<SendInntektsmeldingRequestDto.@Valid RefusjonendringRequestDto> refusjonsendringer,
    @NotNull List<SendInntektsmeldingRequestDto.@Valid BortfaltNaturalytelseRequestDto> bortfaltNaturalytelsePerioder
) {
}
