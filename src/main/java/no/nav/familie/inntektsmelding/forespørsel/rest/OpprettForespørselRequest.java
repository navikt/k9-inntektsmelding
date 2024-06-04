package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.YtelseTypeDto;


public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @NotNull @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull LocalDate skjæringstidspunkt,
                                        @NotNull YtelseTypeDto ytelsetype,
                                        @NotNull @Valid SaksnummerDto saksnummer) {
}
