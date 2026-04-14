package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record HentArbeidsgiverOrganisasjonerResponse(@NotNull @Valid Set<ArbeidsforholdDto> organisasjoner) {
}

