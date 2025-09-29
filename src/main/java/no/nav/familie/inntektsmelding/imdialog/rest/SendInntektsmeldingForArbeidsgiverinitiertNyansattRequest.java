package no.nav.familie.inntektsmelding.imdialog.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.KontaktpersonDto;
import no.nav.familie.inntektsmelding.typer.dto.RefusjonDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record SendInntektsmeldingForArbeidsgiverinitiertNyansattRequest(@Valid UUID foresporselUuid,
                                                                        @NotNull @Valid AktørIdDto aktorId,
                                                                        @NotNull @Valid YtelseTypeDto ytelse,
                                                                        @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                                                        @NotNull @Valid KontaktpersonDto kontaktperson,
                                                                        @NotNull LocalDate startdato,
                                                                        @NotEmpty List<@Valid RefusjonDto> refusjon) {
}
