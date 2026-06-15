package no.nav.k9.inntektsmelding.imapi.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.OmsorgspengerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.RefusjonDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;

public record InntektsmeldingDto(
    @NotNull UUID inntektsmeldingUuid,
    @NotNull UUID forespørselUuid,
    @NotNull @Valid FødselsnummerDto fnr,
    @NotNull @Valid YtelseTypeDto ytelseType,
    @NotNull @Valid OrganisasjonsnummerDto arbeidsgiver,
    @NotNull @Valid KontaktpersonDto kontaktperson,
    @NotNull LocalDate startdato,
    @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
    @NotNull LocalDateTime innsendtTidspunkt,
    @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal refusjonPrMnd,
    LocalDate opphørsdatoRefusjon,
    @NotNull @Valid AvsenderSystemDto avsenderSystem,
    @NotNull List<@Valid RefusjonDto> refusjonsendringer,
    @NotNull List<@Valid BortfaltNaturalytelseDto> bortfaltNaturalytelsePerioder,
    @NotNull List<@Valid EndringsårsakerDto> endringAvInntektÅrsaker,
    @Valid OmsorgspengerDto omsorgspenger
) {

}
