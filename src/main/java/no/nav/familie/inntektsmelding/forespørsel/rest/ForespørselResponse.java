package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record ForespørselResponse(UUID uuid,
                                  OrganisasjonsnummerDto organisasjonsnummer,
                                  LocalDate skjæringstidspunkt,
                                  AktørIdDto brukerAktørId,
                                  YtelseTypeDto ytelseType,
                                  ForespørselStatus status,
                                  List<PeriodeDto> etterspurtePerioder) {
}
