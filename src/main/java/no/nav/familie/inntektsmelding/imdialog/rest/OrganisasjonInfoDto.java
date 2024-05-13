package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.validation.constraints.NotNull;

public record OrganisasjonInfoDto (@NotNull String organisasjonNavn, @NotNull String organisasjonNummer){
}
