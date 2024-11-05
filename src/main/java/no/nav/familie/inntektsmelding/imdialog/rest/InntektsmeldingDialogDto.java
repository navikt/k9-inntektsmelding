package no.nav.familie.inntektsmelding.imdialog.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.familie.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

public record InntektsmeldingDialogDto(@Valid @NotNull InntektsmeldingDialogDto.PersonInfoDto person, @Valid @NotNull InntektsmeldingDialogDto.OrganisasjonInfoDto arbeidsgiver,
                                       @Valid @NotNull InnsenderDto innsender, @Valid @NotNull InntektsopplysningerDto inntektsopplysninger,
                                       @NotNull LocalDate startdatoPermisjon, @Valid @NotNull YtelseTypeDto ytelse,
                                       @Valid @NotNull UUID forespørselUuid, @Valid @NotNull ForespørselStatusDto forespørselStatus,
                                       @Valid SøknadsopplysningerDto søknadsopplysninger) {

    public record PersonInfoDto(@NotNull String fornavn, @NotNull String mellomnavn, @NotNull String etternavn, @NotNull String fødselsnummer,
                                @NotNull String aktørId) {
    }

    public record OrganisasjonInfoDto(@NotNull String organisasjonNavn, @NotNull String organisasjonNummer) {}
    public record SøknadsopplysningerDto(@NotNull @Valid LocalDate førsteFraværsdag, @Valid LocalDate sisteFraværsdag, List<SøknadsperioderDto> perioder) {}
    public record SøknadsperioderDto(@NotNull @Valid LocalDate fom, @Valid LocalDate tom) {}
    public record InnsenderDto(@NotNull String fornavn, String mellomnavn, @NotNull String etternavn, String telefon) {}

    public record InntektsopplysningerDto(@NotNull @Valid BigDecimal gjennomsnittLønn, @NotNull @Valid List<MånedsinntektDto> månedsinntekter){
        public record MånedsinntektDto(@NotNull LocalDate fom, @NotNull LocalDate tom, BigDecimal beløp, @Valid @NotNull MånedslønnStatus status) {}
    }
}
