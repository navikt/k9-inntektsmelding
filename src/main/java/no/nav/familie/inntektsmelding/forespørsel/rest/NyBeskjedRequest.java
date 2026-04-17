package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;

public record NyBeskjedRequest(@NotNull @Valid OrganisasjonsnummerDto orgnr,
                               @NotNull @Valid SaksnummerDto saksnummer,
                               @NotNull LocalDate skjæringstidspunkt) {
}
