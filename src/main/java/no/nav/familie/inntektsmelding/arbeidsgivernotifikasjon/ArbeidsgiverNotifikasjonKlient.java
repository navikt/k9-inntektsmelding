package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import static no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.ArbaidsgiverNotifikasjonErrorHandler.handleError;
import static no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.ArbaidsgiverNotifikasjonErrorHandler.handleValidationError;

import java.net.http.HttpRequest;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResponseProjection;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "arbeidsgiver.notifikasjon.url", endpointDefault = "https://ag-notifikasjon-produsent-api.intern.nav.no", scopesProperty = "arbeidsgiver.notifikasjon.scopes", scopesDefault = "api://prod-gcp.fager.notifikasjon-produsent-api/.default")
class ArbeidsgiverNotifikasjonKlient {

    private static final String ERROR_RESPONSE = "F-102030";

    private final RestClient restKlient;
    private final RestConfig restConfig;

    public ArbeidsgiverNotifikasjonKlient(RestClient restKlient) {
        this.restKlient = restKlient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public String opprettNyOppgave(NyOppgaveMutationRequest request, GraphQLResponseProjection projection) {
        var resultat = query(new GraphQLRequest(request, projection), NyOppgaveResponse.class).nyOppgave();
        if (resultat instanceof NyOppgave nyOppgave) {
            if (!NyOppgave.VELLYKET_TYPENAME.equals(nyOppgave.getTypename())) {
                handleValidationError(nyOppgave.getTypename(), nyOppgave.getFeilmelding(), "opprettelse av ny oppgave");
            }
            return nyOppgave.getId();
        }
        throw new IllegalStateException("Utviklerfeil: Ukjent resultat type.");
    }

    private <T extends GraphQLResult<?>> T query(GraphQLRequest req, Class<T> clazz) {
        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(req.toHttpJsonBody()));
        var restRequest = RestRequest.newRequest(method, restConfig.endpoint(), restConfig);
        var res = restKlient.send(restRequest, clazz);
        if (res.hasErrors()) {
            return handleError(res.getErrors(), restConfig.endpoint(), ERROR_RESPONSE);
        }
        return res;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [endpoint=" + restConfig.endpoint() + "]";
    }
}
