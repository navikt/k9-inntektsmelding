package no.nav.familie.inntektsmelding.imdialog.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.BortfaltNaturalytelseDto;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakerDto;
import no.nav.familie.inntektsmelding.typer.dto.KontaktpersonDto;
import no.nav.familie.inntektsmelding.typer.dto.RefusjonDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record SendInntektsmeldingRequest(@Valid UUID foresporselUuid,
                                         @NotNull @Valid AktørIdDto aktorId,
                                         @NotNull @Valid YtelseTypeDto ytelse,
                                         @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                         @NotNull @Valid KontaktpersonDto kontaktperson,
                                         @NotNull LocalDate startdato,
                                         @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                         @NotNull List<@Valid RefusjonDto> refusjon,
                                         @NotNull List<@Valid BortfaltNaturalytelseDto> bortfaltNaturalytelsePerioder,
                                         @NotNull List<@Valid EndringsårsakerDto> endringAvInntektÅrsaker,
                                         @Valid OmsorgspengerRequestDto omsorgspenger) {
}
