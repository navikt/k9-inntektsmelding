package no.nav.familie.inntektsmelding.forespørsel.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;

public record NyBeskjedRequest(@Valid @NotNull OrganisasjonsnummerDto orgnummer,
                                       @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
}
