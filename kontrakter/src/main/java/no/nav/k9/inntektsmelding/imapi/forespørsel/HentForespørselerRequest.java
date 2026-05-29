package no.nav.k9.inntektsmelding.imapi.forespørsel;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.k9.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;

public record HentForespørselerRequest(@NotNull @Valid OrganisasjonsnummerDto orgnr,
                                       @Valid FødselsnummerDto fnr,
                                       @Valid ForespørselStatusDto status,
                                       @Valid YtelseTypeDto ytelseType,
                                       LocalDate fom,
                                       LocalDate tom) {
}

