package no.nav.familie.inntektsmelding.refusjonomsorgsdager.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;

public record HentInnloggetBrukerRequestDto(@NotNull Ytelsetype ytelseType,
                                            @NotNull @Pattern(regexp = "^\\d{9}$") @Valid String organisasjonsnummer) {
}
