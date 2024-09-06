package no.nav.familie.inntektsmelding.pip;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

@ApplicationScoped
public class PipTjeneste {
    private ForespørselTjeneste forespørselTjeneste;

    PipTjeneste() {
        // CDI proxy
    }

    @Inject
    public PipTjeneste(ForespørselTjeneste forespørselTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
    }

    public OrganisasjonsnummerDto hentOrganisasjonsnummerFor(UUID forespørselUuid) {
        return forespørselTjeneste.finnForespørsel(forespørselUuid).map(f -> new OrganisasjonsnummerDto(f.getOrganisasjonsnummer())).orElse(null);
    }
}
