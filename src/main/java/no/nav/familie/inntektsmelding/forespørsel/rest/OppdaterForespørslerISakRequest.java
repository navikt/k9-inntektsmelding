package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record OppdaterForespørslerISakRequest(@NotNull @Valid AktørIdDto aktørId,
                                              @NotNull @Valid Map<OrganisasjonsnummerDto, List<LocalDate>> stpPerOrg,
                                              @NotNull YtelseTypeDto ytelsetype,
                                              @NotNull @Valid SaksnummerDto saksnummer) {
}
