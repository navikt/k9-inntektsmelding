package no.nav.familie.inntektsmelding.imdialog.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.familie.inntektsmelding.typer.dto.InnsenderDto;
import no.nav.familie.inntektsmelding.typer.dto.InntektsopplysningerDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonInfoDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.PersonInfoDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record InntektsmeldingDialogDto(@Valid @NotNull PersonInfoDto person,
                                       @Valid @NotNull OrganisasjonInfoDto arbeidsgiver,
                                       @Valid @NotNull InnsenderDto innsender,
                                       @Valid @NotNull InntektsopplysningerDto inntektsopplysninger,
                                       @Valid @NotNull LocalDate skjæringstidspunkt,
                                       @Valid @NotNull YtelseTypeDto ytelse,
                                       @Valid UUID forespørselUuid,
                                       @Valid @NotNull ForespørselStatusDto forespørselStatus,
                                       @Valid @NotNull LocalDate førsteUttaksdato,
                                       @Valid List<PeriodeDto> etterspurtePerioder) {
}
