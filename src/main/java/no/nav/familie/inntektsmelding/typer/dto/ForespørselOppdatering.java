package no.nav.familie.inntektsmelding.typer.dto;

import java.util.UUID;

public record ForespørselOppdatering(OppdaterForespørselDto oppdaterDto, UUID forespørselUuid) {
}
