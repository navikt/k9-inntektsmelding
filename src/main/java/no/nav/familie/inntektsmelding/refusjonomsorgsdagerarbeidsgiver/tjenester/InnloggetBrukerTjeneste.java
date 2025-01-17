package no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.refusjonomsorgsdagerarbeidsgiver.rest.InnloggetBrukerDto;
import no.nav.vedtak.exception.ManglerTilgangException;

@ApplicationScoped
public class InnloggetBrukerTjeneste {
    private PersonTjeneste personTjeneste;
    private AltinnTilgangTjeneste altinnTilgangTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(InnloggetBrukerTjeneste.class);

    public InnloggetBrukerTjeneste() {
        // CDI
    }

    @Inject
    public InnloggetBrukerTjeneste(PersonTjeneste personTjeneste, AltinnTilgangTjeneste altinnTilgangTjeneste, OrganisasjonTjeneste organisasjonTjeneste) {
        this.personTjeneste = personTjeneste;
        this.altinnTilgangTjeneste = altinnTilgangTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public InnloggetBrukerDto hentInnloggetBruker(Ytelsetype ytelseType, String organisasjonsnummer) {
        LOG.info("Henter informasjon om innlogget bruker for ytelseType {} og orgnummer {}", ytelseType, organisasjonsnummer);
        var innloggetBruker = personTjeneste.hentInnloggetPerson(ytelseType);
        if (innloggetBruker == null) {
            throw new IllegalStateException("Fant ikke innlogget bruker i PDL.");
        }

        var organisasjon = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer);
        if (organisasjon == null) {
            throw new IllegalArgumentException("Fant ikke organisasjon med orgnummer " + organisasjonsnummer);
        }

        if (altinnTilgangTjeneste.manglerTilgangTilBedriften(organisasjonsnummer)) {
            throw new ManglerTilgangException("MANGLER_TILGANG_FEIL", "Innlogget bruker har ikke tilgang til organisasjonsnummer " + organisasjonsnummer);
        }

        return new InnloggetBrukerDto(
            innloggetBruker.fornavn(),
            innloggetBruker.mellomnavn(),
            innloggetBruker.etternavn(),
            innloggetBruker.telefonnummer(),
            organisasjon.orgnr(),
            organisasjon.navn()
        );
    }
}
