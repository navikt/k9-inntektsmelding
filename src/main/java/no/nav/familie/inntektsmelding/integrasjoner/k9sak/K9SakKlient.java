package no.nav.familie.inntektsmelding.integrasjoner.k9sak;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.fagsak.FagsakInfoDto;
import no.nav.k9.sak.kontrakt.fagsak.MatchFagsak;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, scopesProperty = "k9sak.scopes", scopesDefault = "api://prod-fss.k9saksbehandling.k9-sak/.default", endpointDefault = "http://k9-sak", endpointProperty = "k9sak.url")
public class K9SakKlient {
    private static final String K9SAK_FAKSAKINFO_PATH = "/k9/sak/api/fagsak/match";
    private static final Logger LOG = LoggerFactory.getLogger(K9SakKlient.class);

    private RestClient restClient;
    private RestConfig restConfig;

    public K9SakKlient() {
        // CDI
    }

    public K9SakKlient(RestClient restClient, RestConfig restConfig) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<FagsakInfoDto> hentFagsakInfo(Ytelsetype ytelsetype, String personIdent) {
        URI url = UriBuilder.fromUri(restConfig.endpoint()).path(K9SAK_FAKSAKINFO_PATH).build();
        Periode gyldigPeriode = null; // setter null for å sjekke alle perioder
        PersonIdent bruker = new PersonIdent(personIdent);
        List<PersonIdent> pleietrengende = null; // søker uten pleietrengende
        List<PersonIdent> annenPart = null; // søker uten annen part

        var requestDto = new MatchFagsak(mapYtelsetype(ytelsetype), gyldigPeriode, bruker, pleietrengende, annenPart);
        var request = RestRequest.newPOSTJson(requestDto, url, restConfig);

        try {
            return restClient.sendReturnList(request, FagsakInfoDto.class);
        } catch (Exception e) {
            LOG.error("Kall mot k9-sak feilet for personIdent {}", personIdent, e);
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
