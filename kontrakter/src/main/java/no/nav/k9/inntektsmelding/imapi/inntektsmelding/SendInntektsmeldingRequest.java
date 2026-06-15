package no.nav.k9.inntektsmelding.imapi.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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

public record SendInntektsmeldingRequest(@NotNull @Valid UUID foresporselUuid,
                                         @NotNull @Valid FødselsnummerDto fødselsnummer,
                                         @NotNull @Valid OrganisasjonsnummerDto organisasjonsnummer,
                                         @NotNull LocalDate startdato,
                                         @NotNull YtelseTypeDto ytelseType,
                                         @NotNull @Valid KontaktpersonDto kontaktperson,
                                         @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                         @NotNull List<@Valid RefusjonDto> refusjon,
                                         @NotNull List<@Valid BortfaltNaturalytelseDto> bortfaltNaturalytelsePerioder,
                                         @NotNull List<@Valid EndringsårsakerDto> endringAvInntektÅrsaker,
                                         @NotNull @Valid AvsenderSystemDto avsenderSystem,
                                         @Valid OmsorgspengerDto omsorgspenger) {

    @AssertTrue(message = "ytelseType omsorgspenger må ha omsorgspengerDto")
    private boolean isValidOmsorgspengerInfo() {
        if (ytelseType.equals(YtelseTypeDto.OMSORGSPENGER)) {
            return omsorgspenger != null;
        } else {
            return omsorgspenger == null;
        }
    }
}
