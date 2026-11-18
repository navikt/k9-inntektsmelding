package no.nav.familie.inntektsmelding.pip;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public record ForespørselTilgangInfo(OrganisasjonsnummerDto organisasjonsnummer, AktørIdEntitet aktørId) {
}
