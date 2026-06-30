package no.nav.familie.inntektsmelding.forvaltning.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;

public record ForvaltningForespørselDto(
    UUID uuid,
    LocalDate skjæringstidspunkt,
    String arbeidsgiverident,
    String aktørid,
    String ytelsetype,
    ForespørselStatus status,
    LocalDateTime opprettetTidspunkt,
    UUID dialogportenUuid,
    List<PeriodeDto> etterspurtePerioder) {
}
