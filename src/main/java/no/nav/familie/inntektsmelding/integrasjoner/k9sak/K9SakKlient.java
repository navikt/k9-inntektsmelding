package no.nav.familie.inntektsmelding.integrasjoner.k9sak;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.k9inntektsmelding.FinnSakerDto;
import no.nav.k9.sak.kontrakt.k9inntektsmelding.FinnSakerRequest;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, scopesProperty = "k9sak.scopes", scopesDefault = "api://dev-fss.k9saksbehandling.k9-sak/.default", endpointDefault = "https://k9-sak.dev-fss-pub.nais.io/k9/sak/api", endpointProperty = "k9sak.url")
public class K9SakKlient {
    private static final String K9SAK_FINN_SAKER_PATH = "/k9-inntektsmelding/saker";
    private static final Logger LOG = LoggerFactory.getLogger(K9SakKlient.class);

    private final RestClient restClient;
    private final RestConfig restConfig;

    @Inject
    public K9SakKlient() {
        this(RestClient.client());
    }

    public K9SakKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<FinnSakerDto> hentFagsakInfo(Ytelsetype ytelsetype, AktørId aktørId) {
        URI url = UriBuilder.fromUri(restConfig.endpoint()).path(K9SAK_FINN_SAKER_PATH).build();
        var requestDto = new FinnSakerRequest(aktørId, mapYtelsetype(ytelsetype));
        var request = RestRequest.newPOSTJson(requestDto, url, restConfig);

        try {
            return restClient.sendReturnList(request, FinnSakerDto.class);
        } catch (Exception e) {
            LOG.error("Kall mot k9-sak feilet for", e);
            throw new RuntimeException("Kall mot k9-sak feilet", e);
        }
    }

    private FagsakYtelseType mapYtelsetype(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case PLEIEPENGER_SYKT_BARN -> FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case OMSORGSPENGER -> FagsakYtelseType.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> FagsakYtelseType.OPPLÆRINGSPENGER;
        };
    }
}
