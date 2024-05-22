package no.nav.familie.inntektsmelding.integrasjoner.organisasjon;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class OrganisasjonTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

    private static final LRUCache<String, Organisasjon> CACHE = new LRUCache<>(2500, CACHE_ELEMENT_LIVE_TIME_MS);

    private EregKlient eregRestKlient;

    public OrganisasjonTjeneste() {
        // CDI
    }

    @Inject
    public OrganisasjonTjeneste(EregKlient eregRestKlient) {
        this.eregRestKlient = eregRestKlient;
    }

    /**
     * Henter informasjon fra Enhetsregisteret hvis applikasjonen ikke kjenner til
     * orgnr eller har data som er eldre enn 24 timer.
     *
     * @param orgNummer orgnummeret
     * @return relevant informasjon om virksomheten.
     * @throws IllegalArgumentException ved forespørsel om orgnr som ikke finnes i
     *                                  enhetsreg
     */

    public Optional<Organisasjon> finnOrganisasjon(String orgNummer) {
        if (orgNummer == null) {
            return Optional.empty();
        }
        return OrganisasjonsNummerValidator.erGyldig(orgNummer) ? Optional.of(hent(orgNummer)) : Optional.empty();
    }

    private Organisasjon hent(String orgnr) {
        var virksomhet = Optional.ofNullable(CACHE.get(orgnr)).orElseGet(() -> hentOrganisasjonRest(orgnr));
        CACHE.put(orgnr, virksomhet);
        return virksomhet;
    }

    private Organisasjon hentOrganisasjonRest(String orgNummer) {
        Objects.requireNonNull(orgNummer, "orgNummer");
        var org = eregRestKlient.hentOrganisasjon(orgNummer);
        return new Organisasjon(org.getNavn(), org.organisasjonsnummer());
    }
}
