package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OppdaterForespørselDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record OppdaterForespørslerRequest(@NotNull @Valid AktørIdDto aktørId,
                                          @NotNull List<OppdaterForespørselDto> forespørsler,
                                          @NotNull YtelseTypeDto ytelsetype,
                                          @NotNull @Valid SaksnummerDto saksnummer) {

    @AssertTrue(message = "Hvis ytelsestype er omsorgspenger, må omsorgspengerData være med")
    private boolean isValidOmsorgspengerInfo() {
        if (ytelsetype.equals(YtelseTypeDto.OMSORGSPENGER)) {
            return forespørsler.stream().allMatch(forespørsel -> forespørsel.omsorgspengerData() != null);
        }
        return true;
    }
}

