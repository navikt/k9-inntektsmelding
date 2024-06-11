package no.nav.familie.inntektsmelding.imdialog.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record InntektsmeldingDialogDto(@Valid @NotNull PersonInfoResponseDto person,
                                       @Valid @NotNull OrganisasjonInfoResponseDto arbeidsgiver,
                                       @Valid @NotNull List<MånedsinntektResponsDto> inntekter,
                                       @NotNull LocalDate startdatoPermisjon,
                                       @Valid @NotNull YtelseTypeDto ytelse) {

    public record PersonInfoResponseDto(@NotNull String navn, @NotNull String fødselsnummer, @NotNull String aktørId) {}

    public record OrganisasjonInfoResponseDto(@NotNull String organisasjonNavn, @NotNull String organisasjonNummer) {}

    public record MånedsinntektResponsDto(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull BigDecimal beløp) {}
}
