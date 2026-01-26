package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.sif.abac.kontrakt.abac.AksjonspunktType;
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt;
import no.nav.sif.abac.kontrakt.abac.ResourceType;
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksnummerDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksnummerOperasjonDto;
import no.nav.sif.abac.kontrakt.abac.resultat.TilgangsbeslutningOgSporingshint;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, scopesProperty = "sif-abac-pdp.scopes", scopesDefault = "api://dev-gcp.k9saksbehandling.sif-abac-pdp/.default", endpointDefault = "http://sif-abac-pdp/sif/sif-abac-pdp/api", endpointProperty = "sif-abac-pdp.url")
public class SifAbacPdpKlient {
    private static final String TILGANGSKONTROLL_SAK_PATH = "/tilgangskontroll/v2/k9/sak-sporingshint";
    private static final Logger LOG = LoggerFactory.getLogger(SifAbacPdpKlient.class);

    private final RestClient restClient;
    private final RestConfig restConfig;

    @Inject
    public SifAbacPdpKlient() {
        this(RestClient.client());
    }

    public SifAbacPdpKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public Optional<TilgangsbeslutningOgSporingshint> harAnsattTilgangTilSak(String saksnummer) {
        URI url = UriBuilder.fromUri(restConfig.endpoint()).path(TILGANGSKONTROLL_SAK_PATH).build();
        var requestDto = new SaksnummerOperasjonDto(
            new SaksnummerDto(saksnummer),
            new OperasjonDto(ResourceType.FAGSAK, BeskyttetRessursActionAttributt.CREATE, Set.of(AksjonspunktType.MANUELL)));

        var request = RestRequest.newPOSTJson(requestDto, url, restConfig);

        try {
            return restClient.sendReturnOptional(request, TilgangsbeslutningOgSporingshint.class);
        } catch (Exception e) {
            LOG.error("Kall mot sif-abac-pdp feilet", e);
            throw new RuntimeException("Kall mot sif-abac-pdp feilet", e);
        }
    }
}
