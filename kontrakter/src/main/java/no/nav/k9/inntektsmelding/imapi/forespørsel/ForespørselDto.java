package no.nav.k9.inntektsmelding.imapi.forespørsel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.k9.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.PeriodeDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;

public record ForespørselDto(UUID forespørselUuid,
                             OrganisasjonsnummerDto orgnummer,
                             FødselsnummerDto fødselsnummer,
                             LocalDate skjæringstidspunkt,
                             YtelseTypeDto ytelseType,
                             ForespørselStatusDto status,
                             List<PeriodeDto> etterspurtePerioder,
                             LocalDateTime opprettetTid) {
}

