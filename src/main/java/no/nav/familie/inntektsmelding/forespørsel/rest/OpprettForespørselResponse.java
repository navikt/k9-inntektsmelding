package no.nav.familie.inntektsmelding.forespørsel.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;

public record OpprettForespørselResponse(@NotNull @Valid ForespørselResultat forespørselResultat) {}
