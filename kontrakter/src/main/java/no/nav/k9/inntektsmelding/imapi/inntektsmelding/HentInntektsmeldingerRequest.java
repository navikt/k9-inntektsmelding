package no.nav.k9.inntektsmelding.imapi.inntektsmelding;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;

public record HentInntektsmeldingerRequest(@NotNull @Valid OrganisasjonsnummerDto orgnr,
                                           @Valid FødselsnummerDto fnr,
                                           @Valid YtelseTypeDto ytelseType,
                                           @Valid UUID forespørselUuid,
                                           LocalDate fom,
                                           LocalDate tom) {
}
